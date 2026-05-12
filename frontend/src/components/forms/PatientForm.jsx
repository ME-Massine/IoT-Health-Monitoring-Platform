import { useState, useEffect } from "react";
import { X } from "lucide-react";
import { patientApi } from "../../api/patientApi";

const EMPTY = {
  firstName: "",
  lastName: "",
  age: "",
  gender: "MALE",
  roomNumber: "",
  medicalCondition: "",
};

export function PatientForm({ patient, onSaved, onDeleted, onClose }) {
  const isEdit = !!patient;
  const [fields, setFields] = useState(EMPTY);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    if (patient) {
      setFields({
        firstName: patient.firstName ?? "",
        lastName: patient.lastName ?? "",
        age: patient.age ?? "",
        gender: patient.gender ?? "MALE",
        roomNumber: patient.roomNumber ?? "",
        medicalCondition: patient.medicalCondition ?? "",
      });
    }
  }, [patient]);

  function set(key, value) {
    setFields((prev) => ({ ...prev, [key]: value }));
    setErrors((prev) => ({ ...prev, [key]: undefined }));
  }

  function validate() {
    const e = {};
    if (!fields.firstName.trim()) e.firstName = "Required";
    if (!fields.lastName.trim()) e.lastName = "Required";
    if (!fields.age || isNaN(fields.age) || fields.age < 0 || fields.age > 120)
      e.age = "Enter a valid age (0–120)";
    if (!fields.roomNumber.trim()) e.roomNumber = "Required";
    return e;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) { setErrors(errs); return; }

    setSaving(true);
    try {
      const payload = {
        ...fields,
        age: parseInt(fields.age, 10),
        medicalCondition: fields.medicalCondition.trim() || null,
      };
      const saved = isEdit
        ? await patientApi.update(patient.id, payload)
        : await patientApi.create(payload);
      onSaved(saved);
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!confirmDelete) { setConfirmDelete(true); return; }
    setDeleting(true);
    try {
      await patientApi.delete(patient.id);
      onDeleted(patient.id);
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal-card" role="dialog" aria-modal="true" aria-labelledby="patient-form-title">
        <div className="modal-card__header">
          <h3 id="patient-form-title" className="modal-card__title">
            {isEdit ? "Edit Patient" : "Add Patient"}
          </h3>
          <button className="modal-card__close" onClick={onClose} aria-label="Close">
            <X size={16} />
          </button>
        </div>

        <form className="patient-form" onSubmit={handleSubmit} noValidate>
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="pf-firstName">First name</label>
              <input
                id="pf-firstName"
                value={fields.firstName}
                onChange={(e) => set("firstName", e.target.value)}
                maxLength={100}
              />
              {errors.firstName && <span className="form-field__error">{errors.firstName}</span>}
            </div>
            <div className="form-field">
              <label htmlFor="pf-lastName">Last name</label>
              <input
                id="pf-lastName"
                value={fields.lastName}
                onChange={(e) => set("lastName", e.target.value)}
                maxLength={100}
              />
              {errors.lastName && <span className="form-field__error">{errors.lastName}</span>}
            </div>
          </div>

          <div className="form-row">
            <div className="form-field">
              <label htmlFor="pf-age">Age</label>
              <input
                id="pf-age"
                type="number"
                min={0}
                max={120}
                value={fields.age}
                onChange={(e) => set("age", e.target.value)}
              />
              {errors.age && <span className="form-field__error">{errors.age}</span>}
            </div>
            <div className="form-field">
              <label htmlFor="pf-gender">Gender</label>
              <select
                id="pf-gender"
                value={fields.gender}
                onChange={(e) => set("gender", e.target.value)}
              >
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
              </select>
            </div>
            <div className="form-field">
              <label htmlFor="pf-room">Room number</label>
              <input
                id="pf-room"
                value={fields.roomNumber}
                onChange={(e) => set("roomNumber", e.target.value)}
                maxLength={50}
              />
              {errors.roomNumber && <span className="form-field__error">{errors.roomNumber}</span>}
            </div>
          </div>

          <div className="form-field">
            <label htmlFor="pf-condition">Medical condition <span className="form-field__optional">(optional)</span></label>
            <textarea
              id="pf-condition"
              rows={3}
              maxLength={1000}
              value={fields.medicalCondition}
              onChange={(e) => set("medicalCondition", e.target.value)}
            />
          </div>

          <div className="modal-card__actions">
            {isEdit && (
              <button
                type="button"
                className={`btn btn--danger-outline ${confirmDelete ? "btn--confirm-danger" : ""}`}
                onClick={handleDelete}
                disabled={deleting}
              >
                {deleting ? "Deleting…" : confirmDelete ? "Confirm delete?" : "Delete"}
              </button>
            )}
            <div className="modal-card__actions-right">
              <button type="button" className="btn btn--ghost" onClick={onClose} disabled={saving}>
                Cancel
              </button>
              <button type="submit" className="btn btn--primary" disabled={saving}>
                {saving ? "Saving…" : isEdit ? "Save changes" : "Add patient"}
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
