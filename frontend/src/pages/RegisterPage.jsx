import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import apiClient from "../services/api.js";
import ROUTES from "../constants/routes.js";

/**
 * RegisterPage — Phase 1 placeholder.
 * Full styled implementation with PIN input and validation in T-120 (Phase 10).
 */
export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: "", email: "", phone: "", password: "", pin: "" });
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  function handleChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await apiClient.post("/auth/register", form);
      navigate(ROUTES.LOGIN);
    } catch (err) {
      setError(err.response?.data?.message ?? "Registration failed. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={{ maxWidth: 400, margin: "80px auto", padding: "0 16px" }}>
      <h1>Create account</h1>
      <form onSubmit={handleSubmit}>
        {["name", "email", "phone", "password", "pin"].map((field) => (
          <div key={field} style={{ marginBottom: 12 }}>
            <label htmlFor={field}>{field.charAt(0).toUpperCase() + field.slice(1)}</label>
            <br />
            <input
              id={field}
              name={field}
              type={field === "password" ? "password" : "text"}
              value={form[field]}
              onChange={handleChange}
              required
              maxLength={field === "pin" ? 4 : undefined}
              style={{ width: "100%", padding: 8, marginTop: 4 }}
            />
          </div>
        ))}
        {error && <p style={{ color: "red" }}>{error}</p>}
        <button type="submit" disabled={loading} style={{ width: "100%", padding: 10 }}>
          {loading ? "Registering…" : "Register"}
        </button>
      </form>
      <p>
        Already have an account? <Link to={ROUTES.LOGIN}>Sign in</Link>
      </p>
    </div>
  );
}