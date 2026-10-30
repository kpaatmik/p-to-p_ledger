/**
 * RegisterPage — T-120
 *
 * POST /auth/register
 * Request: { name, email, phone, password, pin }
 * Response 201: { userId, email, name, role, message }
 * Errors: 400 fieldErrors array, 409 duplicate email/phone
 */

import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import apiClient from "../services/api.js";
import ROUTES from "../constants/routes.js";
import "../styles/auth-pages.css";

export default function RegisterPage() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    name: "",
    email: "",
    phone: "",
    password: "",
    pin: "",
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [apiError, setApiError] = useState("");
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    // Clear field error on change
    if (fieldErrors[name]) {
      setFieldErrors((prev) => ({ ...prev, [name]: "" }));
    }
  };

  // Client-side validation mirrors backend Jakarta constraints
  const validate = () => {
    const errs = {};
    if (!form.name.trim()) errs.name = "Name is required";
    if (!form.email.trim()) errs.email = "Email is required";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))
      errs.email = "Enter a valid email address";
    if (!form.phone.trim()) errs.phone = "Phone is required";
    else if (!/^\d{7,20}$/.test(form.phone.replace(/\s/g, "")))
      errs.phone = "Phone must be 7–20 digits";
    if (!form.password) errs.password = "Password is required";
    else if (form.password.length < 8)
      errs.password = "Password must be at least 8 characters";
    if (!form.pin) errs.pin = "PIN is required";
    else if (!/^\d{4}$/.test(form.pin))
      errs.pin = "PIN must be exactly 4 digits";
    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setApiError("");

    const errs = validate();
    if (Object.keys(errs).length > 0) {
      setFieldErrors(errs);
      return;
    }

    setLoading(true);
    try {
      await apiClient.post("/auth/register", form);
      setSuccess(true);
      setTimeout(() => navigate(ROUTES.LOGIN), 2500);
    } catch (err) {
      const data = err?.response?.data;
      if (data?.fieldErrors && Array.isArray(data.fieldErrors)) {
        const map = {};
        data.fieldErrors.forEach((fe) => {
          map[fe.field] = fe.message;
        });
        setFieldErrors(map);
      } else {
        setApiError(data?.message || "Registration failed. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="auth-page">
        <div className="auth-card success">
          <h1>✅ Account Created</h1>
          <p>Redirecting to sign in…</p>
        </div>
      </div>
    );
  }

  const field = (id, label, type = "text", extra = {}) => (
    <div className="form-group">
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        name={id}
        type={type}
        value={form[id]}
        onChange={handleChange}
        disabled={loading}
        {...extra}
      />
      {fieldErrors[id] && (
        <span className="field-error">{fieldErrors[id]}</span>
      )}
    </div>
  );

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>💰 Create Account</h1>
        <form onSubmit={handleSubmit} noValidate>
          {field("name", "Full Name", "text", { placeholder: "Jane Doe", autoComplete: "name" })}
          {field("email", "Email", "email", { placeholder: "jane@example.com", autoComplete: "email" })}
          {field("phone", "Phone Number", "tel", { placeholder: "9876543210" })}
          {field("password", "Password", "password", {
            placeholder: "At least 8 characters",
            autoComplete: "new-password",
          })}
          <div className="form-group">
            <label htmlFor="pin">Transaction PIN</label>
            <input
              id="pin"
              name="pin"
              type="password"
              inputMode="numeric"
              maxLength={4}
              placeholder="4 digits"
              value={form.pin}
              onChange={handleChange}
              disabled={loading}
              autoComplete="new-password"
            />
            {fieldErrors.pin && (
              <span className="field-error">{fieldErrors.pin}</span>
            )}
          </div>

          {apiError && <p className="error-message">{apiError}</p>}

          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading}
          >
            {loading ? "Registering…" : "Create Account"}
          </button>
        </form>
        <p className="auth-link">
          Already have an account? <Link to={ROUTES.LOGIN}>Sign in</Link>
        </p>
      </div>
    </div>
  );
}
