/**
 * MerchantListPage — list of active merchants with search.
 */

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "../components/Layout.jsx";
import { getMerchants } from "../api/merchantService.js";
import ROUTES from "../constants/routes.js";
import "../styles/list-page.css";

export default function MerchantListPage() {
  const navigate = useNavigate();
  const [merchants, setMerchants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");

  useEffect(() => {
    const loadMerchants = async () => {
      setLoading(true);
      setError("");
      try {
        const data = await getMerchants(search);
        setMerchants(Array.isArray(data) ? data : []);
      } catch (err) {
        setError("Failed to load merchants");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    const timeout = setTimeout(() => {
      loadMerchants();
    }, 300);

    return () => clearTimeout(timeout);
  }, [search]);

  return (
    <Layout>
      <div className="list-page">
        <h1>🏪 Merchants</h1>

        <div style={{ marginBottom: "2rem" }}>
          <input
            type="text"
            placeholder="Search merchants..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{
              width: "100%",
              padding: "0.75rem",
              border: "1px solid #d1d5db",
              borderRadius: "4px",
              fontSize: "1rem",
            }}
          />
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        {loading ? (
          <div className="loading-state">Loading merchants...</div>
        ) : merchants.length > 0 ? (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(250px, 1fr))", gap: "1.5rem" }}>
            {merchants.map((merchant) => (
              <div
                key={merchant.id}
                style={{
                  background: "white",
                  borderRadius: "8px",
                  padding: "1.5rem",
                  boxShadow: "0 1px 3px rgba(0, 0, 0, 0.1)",
                  cursor: "pointer",
                  transition: "transform 0.2s, box-shadow 0.2s",
                }}
                onClick={() => navigate(ROUTES.CREATE_RESERVATION, { state: { merchantId: merchant.id } })}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = "translateY(-4px)";
                  e.currentTarget.style.boxShadow = "0 10px 25px rgba(0, 0, 0, 0.1)";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = "translateY(0)";
                  e.currentTarget.style.boxShadow = "0 1px 3px rgba(0, 0, 0, 0.1)";
                }}
              >
                <h3 style={{ margin: "0 0 0.5rem 0", color: "#1a1a1a" }}>
                  {merchant.name}
                </h3>
                <p style={{ margin: "0.5rem 0", color: "#6b7280", fontSize: "0.875rem" }}>
                  {merchant.email}
                </p>
                <div style={{
                  display: "inline-block",
                  backgroundColor: "#d1fae5",
                  color: "#065f46",
                  padding: "0.25rem 0.75rem",
                  borderRadius: "20px",
                  fontSize: "0.75rem",
                  fontWeight: "600",
                  marginTop: "0.5rem",
                }}>
                  {merchant.status}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state">No merchants available</div>
        )}
      </div>
    </Layout>
  );
}
