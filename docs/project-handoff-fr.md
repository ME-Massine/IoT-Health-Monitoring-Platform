---
title: Plateforme de Surveillance Santé IoT — Document de Passation
date: 2026-05-12
tags:
  - iot
  - spring-boot
  - react
  - websocket
  - surveillance-sante
  - passation
status: complet
---

# Plateforme de Surveillance Santé IoT

> [!abstract] Présentation du Projet
> Un **système de surveillance médicale en temps réel** qui simule des dispositifs médicaux IoT connectés transmettant les constantes vitales de patients (fréquence cardiaque, température, SpO2) à une plateforme centrale. Le système surveille en continu les données reçues, détecte automatiquement les valeurs dangereuses, déclenche des alertes cliniques et pousse tout en direct sur un tableau de bord web — sans rechargement de page.

---

## Architecture

```
Simulateur Python ──HTTP POST──► Spring Boot (8080) ◄──REST + WebSocket──► Frontend React (5173)
mqtt_publisher.py ──MQTT pub──►  Broker MQTT (1883)         │
                                        │  ◄──MQTT sub───────┘
                                   PostgreSQL (5432)
```

| Couche | Technologie | Port |
| --- | --- | --- |
| Frontend (Interface) | React 19 + Vite | 5173 |
| Backend (API) | Spring Boot 3.5 · Java 17 | 8080 |
| Base de données | PostgreSQL | 5432 |
| Simulateur | Python 3.12 | — (client uniquement) |
| Broker MQTT | HiveMQ public / Mosquitto local | 1883 |

---

## Décomposition Couche par Couche

### 1. Simulateur — `simulator/`

**Rôle :** Imite des dispositifs IoT physiques connectés aux patients.

- Envoie des relevés de constantes vitales via `HTTP POST /api/v1/vitals` toutes les quelques secondes
- Les constantes **évoluent de façon réaliste** — elles ne varient pas aléatoirement mais se rapprochent progressivement des plages normales, imitant la physiologie réelle
- **Taux d'injection d'anomalies** configurable (ex. 10 %) qui déclenche l'un des 8 scénarios prédéfinis : `high_hr_critical`, `spo2_warning`, `fever`, etc.
- Valide les codes de dispositifs auprès de l'API au démarrage et ignore ceux qui n'existent pas en base
- Inclut un script `mqtt_publisher.py` séparé qui publie les relevés vers un broker MQTT au lieu de HTTP

> [!tip] Pourquoi Python ?
> Prototypage rapide, code minimal, et les bibliothèques `requests` et `paho-mqtt` rendent les appels HTTP/MQTT triviaux. Le simulateur est uniquement un producteur de données.

---

### 2. Backend — `backend/`

**Rôle :** Le cerveau du système. Gère l'ingestion, la logique métier, la persistance, l'API REST et la diffusion en temps réel.

#### Structure des Packages

| Package | Responsabilité |
| --- | --- |
| `controller/` | Couche REST légère — reçoit les requêtes, délègue immédiatement aux services |
| `service/` | Logique métier — pipeline d'ingestion, évaluation des alertes |
| `entity/` | Entités JPA mappées aux tables PostgreSQL |
| `websocket/` | Publishers qui poussent les événements vers les clients frontend connectés |
| `dto/` + `mapper/` | Sépare les formats API des entités internes |
| `mqtt/` | Chemin d'ingestion MQTT optionnel (conditionnel à `app.mqtt.enabled`) |

#### Pipeline d'Ingestion (par relevé)

Deux chemins d'ingestion existent — HTTP et MQTT — mais ils convergent vers la même méthode de service :

```
Chemin HTTP :
  POST /api/v1/vitals
    └─► VitalSignService.ingestVitalSign()

Chemin MQTT :
  Topic broker : iot-health/devices/{deviceCode}/vitals
    └─► MqttVitalSignListener.messageArrived()
          └─► VitalSignService.ingestVitalSign()   ← même pipeline

VitalSignService.ingestVitalSign() :
  ├─ Résoudre le code dispositif → trouver le patient
  ├─ Sauvegarder VitalSign en PostgreSQL
  ├─ Appeler AlertService → évaluer les seuils → sauvegarder Alert si dépassé
  └─ Publier via WebSocket :
        /topic/vitals
        /topic/patients/{id}/vitals
        /topic/alerts          (si alerte créée)
        /topic/patients/{id}/alerts
```

#### Seuils d'Alerte

> [!warning] Logique Clinique (reproduite dans le frontend `vitalStatus.js`)

| Constante | Avertissement | Critique |
| --- | --- | --- |
| Fréquence cardiaque | ≥ 110 bpm | < 50 ou > 120 bpm |
| Température | ≥ 37,8 °C | < 35,0 ou > 38,0 °C |
| SpO2 | ≤ 94 % | ≤ 92 % |

#### Entités

- **`Patient`** — prénom, nom, âge, genre, numéro de chambre, condition médicale
- **`Device`** — code dispositif (chaîne), patient associé (FK), statut, type
- **`VitalSign`** — FC, temp, SpO2, horodatage, dispositif (FK), patient (FK)
- **`Alert`** — sévérité (`WARNING` / `CRITICAL`), type, message, indicateur de résolution, `resolvedAt`, `acknowledgedAt`, patient (FK)

> [!note] Cycle de vie d'une alerte
> Une alerte dispose désormais de trois états au-delà de sa création :
> - **Acquittée** — un clinicien l'a vue (`PATCH /alerts/{id}/acknowledge` renseigne `acknowledgedAt`)
> - **Résolue** — la condition est réglée (`PUT /alerts/{id}/resolve` renseigne `resolvedAt`, `resolved = true`)
> - **Rejetée** — faux positif ou bruit, supprimée définitivement du système (`DELETE /alerts/{id}`)

> [!note] Performance
> Toutes les relations utilisent `FetchType.LAZY` — les données ne sont chargées depuis la base que lorsqu'elles sont explicitement accédées, évitant ainsi les problèmes de requêtes N+1. Les tables `VitalSign` et `Alert` disposent d'indices composites pour des requêtes sur plage temporelle efficaces.

#### Topics WebSocket

| Topic | Contenu |
| --- | --- |
| `/topic/vitals` | Chaque nouveau relevé de constantes (tous patients) |
| `/topic/patients/{id}/vitals` | Constantes spécifiques à un patient |
| `/topic/alerts` | Chaque nouvelle alerte |
| `/topic/patients/{id}/alerts` | Alertes spécifiques à un patient |

#### Ingestion MQTT (`mqtt/`)

Quatre classes implémentent le second chemin d'ingestion optionnel :

| Classe | Rôle |
| --- | --- |
| `MqttProperties` | `@ConfigurationProperties(prefix = "app.mqtt")` — URL broker, ID client, pattern de topic, QoS |
| `MqttConfig` | `@ConditionalOnProperty(enabled=true)` — active tout le sous-système |
| `MqttPayload` | Record de désérialisation JSON pour les messages MQTT entrants |
| `MqttVitalSignListener` | Connexion au `@PostConstruct`, abonnement, analyse chaque message en `VitalSignRequest`, appel de `VitalSignService.ingestVitalSign()` |

Convention de topic : `iot-health/devices/{deviceCode}/vitals` (wildcard `+` en souscription).

Activation dans `application.yml` :
```yaml
app:
  mqtt:
    enabled: true
    broker-url: tcp://broker.hivemq.com:1883
    client-id: iot-health-backend
    topic-pattern: iot-health/devices/+/vitals
    qos: 1
```

Pour voir l'activité MQTT dans les logs :
```yaml
logging:
  level:
    com.iothealth.backend.mqtt: DEBUG
```

---

### 3. Base de Données — PostgreSQL

- Schéma **géré automatiquement par Hibernate** (`ddl-auto: update`) — les tables sont créées/mises à jour au démarrage, sans scripts SQL de migration
- Stocke **l'historique complet** de tous les relevés et alertes (les alertes rejetées sont supprimées via DELETE)
- Indices composites sur `VitalSign` et `Alert` pour des requêtes paginées efficaces
- La journalisation SQL est désactivée (`show-sql: false`) pour garder la console propre en utilisation normale

---

### 4. Frontend — `frontend/`

**Rôle :** Tableau de bord clinique composé de trois pages, toutes mises à jour en direct via WebSocket.

#### Pages

##### Tableau de Bord `/`
- **Bande KPI** — total patients · nombre critiques · nombre avertissements · dispositifs en ligne · **courbe de tendance des alertes sur 24 h** (graphique en aires, séries avertissement + critique)
- **Grille de cartes patients** — FC, Temp, SpO2 actuels avec bordure colorée selon le statut
  - 🔴 Rouge = CRITIQUE · 🟠 Orange = AVERTISSEMENT · 🟢 Vert = STABLE
- **Onglets de filtre** — Tous / Critiques / Avertissements / Stables (cartes critiques en premier)
- **Bouton Ajouter un patient** — ouvre le modal de création de patient
- **Icône de modification par carte** — survol pour révéler un bouton crayon ouvrant le modal d'édition/suppression
- Cartes mises à jour **en direct** sans rechargement

##### Détail Patient `/patients/:id`
- **3 cartes métriques** — valeur actuelle + badge de statut (Normal / Élevé / Bas) par constante, plus un **indicateur de delta** (▲ rouge / ▼ bleu / ↔ gris) montrant l'évolution par rapport au relevé précédent
- **Sélecteur de plage temporelle** — préréglages En direct · 1 h · 6 h · 24 h au-dessus des graphiques ; la sélection d'un préréglage recharge l'historique complet pour cette fenêtre
- **Graphiques linéaires** (Recharts) — un par constante, affichant l'historique avec des lignes de référence aux seuils avertissement/critique ; les fenêtres de maintenance sont ombrées
- **Frise chronologique des alertes** — timeline verticale de toutes les alertes du patient, colorées par sévérité

##### Centre d'Alertes `/alerts`
- **Barre de résumé** — nombre critiques · nombre avertissements · nombre résolues
- **Onglets de filtre** — Non résolues / Critiques / Avertissements / Toutes
- **Liste d'alertes** — badge sévérité, type, message, lien patient, horodatage
- **Boutons d'action par alerte :**
  - **Acquitter** (icône œil) — marque l'alerte comme vue ; le tag « Acquittée » apparaît ; le bouton disparaît après usage
  - **Résoudre** (icône coche) — clôture l'alerte ; masqué après résolution
  - **Rejeter** (icône corbeille) — supprime définitivement l'alerte (faux positifs / bruit)
- Les nouvelles alertes apparaissent en haut **instantanément** via WebSocket

#### Architecture Temps Réel (Frontend)

Chaque page utilise un hook React personnalisé qui fait deux choses au montage :

1. **Chargement initial** — appel REST API pour alimenter l'état
2. **Abonnement WebSocket** — abonnement STOMP qui met à jour l'état à chaque nouvelle donnée

```
usePatients()       → GET /patients  +  subscribe /topic/vitals
useAlerts()         → GET /alerts    +  subscribe /topic/alerts
usePatientDetail()  → GET /vitals/patient/{id}/history  +  subscribe /topic/patients/{id}/vitals
                                                         +  subscribe /topic/patients/{id}/alerts
```

Le **badge d'alertes dans la barre latérale** se met à jour via trois chemins :
- Événement WebSocket nouvelle alerte → incrément immédiat
- Clic sur Résoudre/Rejeter → `CustomEvent("alert-resolved")` → décrément immédiat
- Polling toutes les 30 s → correction de dérive (gère les résolutions externes)

#### Gestion Globale des Erreurs

Un intercepteur de réponse Axios dans `httpClient.js` capture toutes les erreurs API non-404 et dispatch un `CustomEvent("api-error", { detail: message })`. `AppLayout` écoute cet événement et pousse un toast `ERROR`, informant l'utilisateur de tout échec backend sans nécessiter de gestion d'erreur dans chaque composant.

#### CRUD Patient

Un modal `PatientForm.jsx` gère la création, l'édition et la suppression :
- **Création** — le bouton « Ajouter un patient » dans l'en-tête du tableau de bord ouvre un formulaire vierge
- **Édition** — l'icône crayon sur chaque carte patient ouvre le formulaire pré-rempli
- **Suppression** — confirmation en deux étapes dans le modal d'édition (clic 1 → « Confirmer la suppression ? » → clic 2)
- En cas de succès, les fonctions `addPatient` / `updatePatient` / `removePatient` du hook mettent à jour l'état localement sans rechargement complet

---

## Flux de Données de Bout en Bout

```
Chemin HTTP :
  1.  Le simulateur envoie :
        POST /api/v1/vitals
        { deviceCode, heartRate, temperature, spo2 }

Chemin MQTT :
  1.  mqtt_publisher.py publie :
        topic : iot-health/devices/{deviceCode}/vitals
        payload : { deviceCode, heartRate, temperature, spo2, recordedAt }

Les deux chemins convergent vers :
  2.  VitalSignService.ingestVitalSign()
        a. Résoudre le dispositif par code → identifier le patient
        b. Persister VitalSign en PostgreSQL
        c. AlertService : vérifier les seuils → persister Alert si dépassé
        d. Publier les événements WebSocket (constante + alerte éventuelle)

  3.  Frontend React (connecté via STOMP) :
        usePatients()      → met à jour la carte patient en direct (valeurs + delta)
        useAlerts()        → ajoute l'alerte en tête de liste
        AppLayout          → incrémente le badge dans la barre latérale
```

---

## Choix de Conception

> [!question] Pourquoi WebSocket plutôt que le polling ?
> Le polling (ex. toutes les 5 s) gaspille de la bande passante et ajoute de la latence. Le WebSocket maintient une connexion TCP persistante — le serveur **pousse** les données dès qu'elles sont disponibles. Critique dans un contexte de surveillance médicale où les secondes comptent.

> [!question] Pourquoi STOMP plutôt qu'un WebSocket brut ?
> Un WebSocket brut n'est qu'un tube d'octets — il faudrait inventer son propre protocole de messages. STOMP apporte une sémantique **publication/abonnement** avec des topics nommés, ce qui correspond naturellement à « s'abonner aux constantes de ce patient ». Spring dispose d'un support STOMP natif via `spring-boot-starter-websocket`.

> [!question] Pourquoi deux chemins d'ingestion (HTTP et MQTT) ?
> HTTP est plus simple et fonctionne partout. MQTT est le protocole standard pour le matériel IoT réel — il est léger, conçu pour les réseaux peu fiables, et prend en charge les garanties QoS. Les deux chemins transitent par le même `VitalSignService`, donc la logique métier n'est jamais dupliquée.

> [!question] Pourquoi un simulateur séparé plutôt que des données figées ?
> Démonstration réaliste et testabilité. Le simulateur génère une dérive physiologiquement plausible avec une **injection d'anomalies contrôlée**. Cela permet de démontrer le déclenchement du système d'alertes en temps réel sans matériel physique.

> [!question] Pourquoi PostgreSQL et non une base time-series comme InfluxDB ?
> InfluxDB est plus optimal pour les requêtes purement temporelles, mais ce système comporte aussi des données relationnelles (patients, dispositifs, alertes avec clés étrangères). PostgreSQL couvre les deux avec un indexage approprié, sans complexité opérationnelle supplémentaire pour une démonstration.

> [!question] Comment le frontend connaît-il les seuils sans appeler le backend ?
> Les seuils de `AlertService` sont **reproduits** dans `frontend/src/utils/vitalStatus.js`. Backend et frontend utilisent les mêmes valeurs numériques, donc le code couleur de l'interface correspond toujours à ce qui a réellement généré une alerte.

> [!question] Que se passe-t-il si le backend tombe ?
> Le frontend affiche un état d'erreur capturé dans les blocs `try/catch` des hooks. Le client STOMP (`@stomp/stompjs`) dispose d'une logique de reconnexion intégrée et se réabonnera automatiquement au retour du backend.

> [!question] Pourquoi CORS est-il configuré avec PATCH dans allowedMethods ?
> L'endpoint d'acquittement des alertes utilise `PATCH`. Les navigateurs envoient une requête preflight `OPTIONS` avant toute méthode non simple. Si `PATCH` est absent de `allowedMethods` dans `WebConfig.java`, le preflight échoue silencieusement et la requête n'atteint jamais le backend. Il faut toujours inclure chaque méthode HTTP utilisée par l'API.

---

## Ce que Ce Projet N'Est PAS (Limites Intentionnelles)

> [!warning] Pas Prêt pour la Production — par Conception

| Fonctionnalité manquante | Pourquoi acceptable pour la démo |
| --- | --- |
| Authentification / JWT | Démo uniquement — pas de comptes utilisateurs nécessaires |
| HTTPS / TLS | Réseau local uniquement |
| Déduplication des alertes | Un FC élevé persistant génère une alerte par relevé — volume acceptable en démo |
| Mise à l'échelle horizontale | Instance backend unique |
| Journaux d'audit | Hors périmètre |
| Contrôle d'accès par rôle | Vue opérateur unique |

Un système clinique en production nécessiterait tout ceci, plus la conformité (RGPD/HIPAA), la redondance et une certification formelle des dispositifs.

---

## Lancer l'Application en Local

```powershell
# Terminal 1 — Backend
cd backend
.\mvnw.cmd spring-boot:run

# Terminal 2 — Frontend
cd frontend
npm install
npm run dev

# Terminal 3 — Simulateur HTTP
cd simulator
.\venv\Scripts\Activate.ps1
python simulator.py

# Terminal 4 (optionnel) — Éditeur MQTT
cd simulator
.\venv\Scripts\Activate.ps1
python mqtt_publisher.py --loop --anomaly
```

Ouvrir **http://localhost:5173** dans le navigateur.

> [!tip] Première installation
> Copier `backend/src/main/resources/application.example.yml` → `application.yml` et renseigner le mot de passe PostgreSQL.
> Copier `frontend/.env.example` → `.env.local` (les valeurs par défaut fonctionnent telles quelles en local).
> Installer les dépendances du simulateur : `pip install -r requirements.txt` (inclut `paho-mqtt`).

> [!tip] MQTT sans Docker
> Le projet est préconfiguré pour utiliser le broker public gratuit HiveMQ (`broker.hivemq.com:1883`). Aucune installation locale de broker nécessaire. Définir `app.mqtt.enabled: true` dans `application.yml` et redémarrer le backend.

---

## Référence API REST

Chemin de base : `http://localhost:8080/api/v1`
Interface Swagger : `http://localhost:8080/swagger-ui/index.html`

### Constantes Vitales

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/vitals` | Ingérer un relevé de constantes |
| `GET` | `/vitals/patient/{id}/history` | Historique paginé (params `from`, `to`, `limit` ; max 500) |

### Patients

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/patients` | Lister tous les patients |
| `GET` | `/patients/{id}` | Obtenir un patient |
| `POST` | `/patients` | Créer un nouveau patient |
| `PUT` | `/patients/{id}` | Mettre à jour un patient |
| `DELETE` | `/patients/{id}` | Supprimer un patient |

### Alertes

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/alerts` | Lister toutes les alertes (ordre décroissant par date) |
| `GET` | `/alerts/unresolved` | Alertes non résolues uniquement |
| `GET` | `/alerts/patient/{patientId}` | Toutes les alertes d'un patient spécifique |
| `GET` | `/alerts/summary?from=&to=` | Nombre horaire d'alertes (critique + avertissement) entre deux horodatages |
| `PUT` | `/alerts/{id}/resolve` | Marquer une alerte comme résolue |
| `PATCH` | `/alerts/{id}/acknowledge` | Acquitter une alerte (renseigne `acknowledgedAt`) |
| `DELETE` | `/alerts/{id}` | Rejeter (supprimer définitivement) une alerte |

### Dispositifs

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/devices` | Lister tous les dispositifs enregistrés |
| `GET` | `/devices/code/{code}` | Rechercher un dispositif par son code |

---

## Carte des Fichiers (Fichiers Clés Uniquement)

```
backend/src/main/java/com/iothealth/backend/
├── controller/
│   ├── AlertController.java       résoudre, acquitter, rejeter, résumé
│   ├── PatientController.java     CRUD complet
│   └── VitalSignController.java   ingestion + historique
├── service/
│   ├── VitalSignService.java      ingestion + publication WS
│   └── AlertService.java          évaluation des seuils + méthodes de cycle de vie
├── entity/
│   ├── Alert.java                 inclut le champ acknowledgedAt + méthode acknowledge()
│   ├── Patient.java
│   ├── Device.java
│   └── VitalSign.java
├── dto/alert/
│   ├── AlertResponse.java         inclut acknowledgedAt
│   └── AlertSummaryPoint.java     bucket horaire (hour, critical, warning)
├── config/
│   └── WebConfig.java             CORS — inclut PATCH dans allowedMethods
├── websocket/                     Publishers WebSocket
└── mqtt/
    ├── MqttConfig.java            activation conditionnelle du bean
    ├── MqttProperties.java        URL broker, ID client, topic, QoS
    ├── MqttPayload.java           record de désérialisation JSON
    └── MqttVitalSignListener.java connexion → abonnement → ingestion

frontend/src/
├── api/
│   ├── httpClient.js              instance Axios + intercepteur d'erreur (CustomEvent)
│   ├── patientApi.js              getAll, getById, create, update, delete
│   ├── alertApi.js                getAll, resolve, acknowledge, dismiss, getSummary
│   ├── vitalSignApi.js            getHistoryByPatientId (supporte from/to/limit)
│   └── wsClient.js                factory STOMP
├── hooks/
│   ├── usePatients.js             fetch + WS + addPatient/updatePatient/removePatient
│   ├── useAlerts.js               fetch + WS + handleAlertUpdated/handleAlertDismissed
│   └── usePatientDetail.js        fetch(rangeHours) + WS constantes + WS alertes
├── pages/
│   ├── DashboardPage.jsx          grille patients + bouton Ajouter + PatientForm
│   ├── PatientDetailPage.jsx      constantes + sélecteur plage + graphiques + alertes
│   └── AlertCenterPage.jsx        liste alertes + actions acquitter/résoudre/rejeter
├── components/
│   ├── layout/
│   │   ├── AppLayout.jsx          barre latérale + événement api-error → toast
│   │   └── ToastContainer.jsx     sévérités ERROR / CRITICAL / WARNING / INFO
│   ├── dashboard/
│   │   ├── PatientCard.jsx        bouton d'édition au survol
│   │   └── KpiStrip.jsx           4 cartes KPI + AlertSparkline (graphique aires 24 h)
│   ├── detail/
│   │   ├── VitalCard.jsx          valeur + indicateur delta (▲/▼/↔)
│   │   ├── VitalChart.jsx         graphique Recharts + lignes de référence + maintenance
│   │   └── PatientAlerts.jsx      frise chronologique des alertes
│   └── forms/
│       └── PatientForm.jsx        modal création/édition/suppression avec validation
├── utils/vitalStatus.js           utilitaires de seuils (miroir de AlertService)
└── styles/global.css              tous les styles

simulator/
├── simulator.py                   simulateur HTTP avec injection d'anomalies
└── mqtt_publisher.py              éditeur MQTT (batch unique ou --loop --anomaly)
```

---

#iot #spring-boot #react #websocket #postgresql #mqtt #surveillance-sante #passation
