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
                                        │
                                   PostgreSQL (5432)
```

| Couche | Technologie | Port |
| --- | --- | --- |
| Frontend (Interface) | React 19 + Vite | 5173 |
| Backend (API) | Spring Boot 3.5 · Java 17 | 8080 |
| Base de données | PostgreSQL | 5432 |
| Simulateur | Python 3.12 | — (client uniquement) |

---

## Décomposition Couche par Couche

### 1. Simulateur — `simulator/`

**Rôle :** Imite des dispositifs IoT physiques connectés aux patients.

- Envoie des relevés de constantes vitales via `HTTP POST /api/v1/vitals` toutes les quelques secondes
- Les constantes **évoluent de façon réaliste** — elles ne varient pas aléatoirement mais se rapprochent progressivement des plages normales, imitant la physiologie réelle
- **Taux d'injection d'anomalies** configurable (ex. 10 %) qui déclenche l'un des 8 scénarios prédéfinis : `high_hr_critical`, `spo2_warning`, `fever`, etc.
- Valide les codes de dispositifs auprès de l'API au démarrage et ignore ceux qui n'existent pas en base

> [!tip] Pourquoi Python ?
> Prototypage rapide, code minimal, et la bibliothèque `requests` rend les appels HTTP triviaux. Pas besoin de Java ou Node ici — le simulateur est uniquement un producteur de données.

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

#### Pipeline d'Ingestion (par relevé)

```
POST /api/v1/vitals
  └─► VitalSignService.ingestVitalSign()
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

- **`Patient`** — nom, numéro de chambre
- **`Device`** — code dispositif (chaîne), patient associé (FK)
- **`VitalSign`** — FC, temp, SpO2, horodatage, dispositif (FK), patient (FK)
- **`Alert`** — sévérité (`WARNING` / `CRITICAL`), type, message, indicateur de résolution, patient (FK)

> [!note] Performance
> Toutes les relations utilisent `FetchType.LAZY` — les données ne sont chargées depuis la base que lorsqu'elles sont explicitement accédées, évitant ainsi les problèmes de requêtes N+1. Les tables `VitalSign` et `Alert` disposent d'indices composites pour des requêtes sur plage temporelle efficaces.

#### Topics WebSocket

| Topic | Contenu |
| --- | --- |
| `/topic/vitals` | Chaque nouveau relevé de constantes (tous patients) |
| `/topic/patients/{id}/vitals` | Constantes spécifiques à un patient |
| `/topic/alerts` | Chaque nouvelle alerte |
| `/topic/patients/{id}/alerts` | Alertes spécifiques à un patient |

---

### 3. Base de Données — PostgreSQL

- Schéma **géré automatiquement par Hibernate** (`ddl-auto: update`) — les tables sont créées/mises à jour au démarrage, sans scripts SQL de migration
- Stocke **l'historique complet** de tous les relevés et alertes (y compris les alertes résolues)
- Indices composites sur `VitalSign` et `Alert` pour des requêtes paginées efficaces

---

### 4. Frontend — `frontend/`

**Rôle :** Tableau de bord clinique composé de trois pages, toutes mises à jour en direct via WebSocket.

#### Pages

##### Tableau de Bord `/`
- **Bande KPI** — total patients · nombre critiques · nombre avertissements · dispositifs en ligne
- **Grille de cartes patients** — FC, Temp, SpO2 actuels avec bordure colorée selon le statut
  - 🔴 Rouge = CRITIQUE · 🟠 Orange = AVERTISSEMENT · 🟢 Vert = STABLE
- **Onglets de filtre** — Tous / Critiques / Avertissements / Stables (cartes critiques en premier)
- Cartes mises à jour **en direct** sans rechargement

##### Détail Patient `/patients/:id`
- **3 cartes métriques** — valeur actuelle + badge de statut (Normal / Élevé / Bas) par constante
- **Graphiques linéaires** (Recharts) — un par constante, affichant l'historique récent avec des lignes de référence aux seuils avertissement/critique
- **Frise chronologique des alertes** — timeline verticale de toutes les alertes du patient, colorées par sévérité

##### Centre d'Alertes `/alerts`
- **Barre de résumé** — nombre critiques · nombre avertissements · nombre résolues
- **Onglets de filtre** — Non résolues / Critiques / Avertissements / Toutes
- **Liste d'alertes** — badge sévérité, type, message, lien patient, horodatage, bouton Résoudre
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
- Clic sur le bouton Résoudre → `CustomEvent("alert-resolved")` → décrément immédiat
- Polling toutes les 30 s → correction de dérive (gère les résolutions externes)

---

## Flux de Données de Bout en Bout

```
1.  Le simulateur envoie :
      POST /api/v1/vitals
      { deviceCode, heartRate, temperature, spo2 }

2.  VitalSignController → VitalSignService.ingestVitalSign()

3.  VitalSignService :
      a. Résoudre le dispositif par code → identifier le patient
      b. Persister VitalSign en PostgreSQL
      c. AlertService : vérifier les seuils → persister Alert si dépassé
      d. Publier les événements WebSocket (constante + alerte éventuelle)

4.  Frontend React (connecté via STOMP) :
      usePatients()      → met à jour la carte patient en direct
      useAlerts()        → ajoute l'alerte en tête de liste
      AppLayout          → incrémente le badge dans la barre latérale
```

---

## Choix de Conception

> [!question] Pourquoi WebSocket plutôt que le polling ?
> Le polling (ex. toutes les 5 s) gaspille de la bande passante et ajoute de la latence. Le WebSocket maintient une connexion TCP persistante — le serveur **pousse** les données dès qu'elles sont disponibles. Critique dans un contexte de surveillance médicale où les secondes comptent.

> [!question] Pourquoi STOMP plutôt qu'un WebSocket brut ?
> Un WebSocket brut n'est qu'un tube d'octets — il faudrait inventer son propre protocole de messages. STOMP apporte une sémantique **publication/abonnement** avec des topics nommés, ce qui correspond naturellement à « s'abonner aux constantes de ce patient ». Spring dispose d'un support STOMP natif via `spring-boot-starter-websocket`.

> [!question] Pourquoi un simulateur séparé plutôt que des données figées ?
> Démonstration réaliste et testabilité. Le simulateur génère une dérive physiologiquement plausible avec une **injection d'anomalies contrôlée**. Cela permet de démontrer le déclenchement du système d'alertes en temps réel sans matériel physique.

> [!question] Pourquoi PostgreSQL et non une base time-series comme InfluxDB ?
> InfluxDB est plus optimal pour les requêtes purement temporelles, mais ce système comporte aussi des données relationnelles (patients, dispositifs, alertes avec clés étrangères). PostgreSQL couvre les deux avec un indexage approprié, sans complexité opérationnelle supplémentaire pour une démonstration.

> [!question] Comment le frontend connaît-il les seuils sans appeler le backend ?
> Les seuils de `AlertService` sont **reproduits** dans `frontend/src/utils/vitalStatus.js`. Backend et frontend utilisent les mêmes valeurs numériques, donc le code couleur de l'interface correspond toujours à ce qui a réellement généré une alerte.

> [!question] Que se passe-t-il si le backend tombe ?
> Le frontend affiche un état d'erreur capturé dans les blocs `try/catch` des hooks. Le client STOMP (`@stomp/stompjs`) dispose d'une logique de reconnexion intégrée et se réabonnera automatiquement au retour du backend.

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

# Terminal 3 — Simulateur
cd simulator
.\venv\Scripts\Activate.ps1
python simulator.py
```

Ouvrir **http://localhost:5173** dans le navigateur.

> [!tip] Première installation
> Copier `backend/src/main/resources/application.example.yml` → `application.yml` et renseigner le mot de passe PostgreSQL.
> Copier `frontend/.env.example` → `.env.local` (les valeurs par défaut fonctionnent telles quelles en local).

---

## Référence API REST

Chemin de base : `http://localhost:8080/api/v1`
Interface Swagger : `http://localhost:8080/swagger-ui/index.html`

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/vitals` | Ingérer un relevé de constantes |
| `GET` | `/vitals/patient/{id}/history` | Historique paginé (params `from`, `to`, `limit` ; max 500) |
| `GET` | `/patients` | Lister tous les patients |
| `GET` | `/alerts` | Lister toutes les alertes |
| `GET` | `/alerts/unresolved` | Alertes non résolues uniquement |
| `PUT` | `/alerts/{id}/resolve` | Marquer une alerte comme résolue |
| `GET` | `/devices` | Lister tous les dispositifs enregistrés |
| `GET` | `/devices/code/{code}` | Rechercher un dispositif par son code |

---

## Carte des Fichiers (Fichiers Clés Uniquement)

```
backend/src/main/java/com/iothealth/backend/
├── controller/          Endpoints REST
├── service/
│   ├── VitalSignService.java    ingestion + publication WS
│   └── AlertService.java        évaluation des seuils
├── entity/              Entités JPA (Patient, Device, VitalSign, Alert)
├── websocket/           Publishers WebSocket
└── mqtt/                chemin d'ingestion MQTT optionnel

frontend/src/
├── api/                 Clients Axios (patientApi, alertApi, deviceApi, wsClient)
├── hooks/               usePatients, useAlerts, usePatientDetail, useGlobalAlertsSocket
├── pages/               DashboardPage, PatientDetailPage, AlertCenterPage
├── components/
│   ├── layout/          AppLayout (barre latérale + badge d'alertes)
│   ├── dashboard/       PatientCard, KpiStrip
│   └── detail/          VitalCard, VitalChart, PatientAlerts
├── utils/vitalStatus.js utilitaires de seuils (miroir de AlertService)
└── styles/global.css    tous les styles

simulator/
└── simulator.py         simulateur de dispositifs avec injection d'anomalies
```

---

#iot #spring-boot #react #websocket #postgresql #surveillance-sante #passation
