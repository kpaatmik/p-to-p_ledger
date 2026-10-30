/**
 * AdminUsersPage — admin user and wallet management.
 *
 * Freeze/unfreeze calls PATCH /wallet/admin/wallets/{walletId}/status.
 * walletId comes from AdminUserResponse.walletId (UUID).
 */

import { useEffect, useState } from "react";
import AdminLayout from "../../components/AdminLayout.jsx";
import { getAdminUsers } from "../../api/adminService.js";
import { freezeWallet, unfreezeWallet } from "../../api/walletService.js";
import { formatINR } from "../../utils/currency.js";
import "../../styles/admin-list.css";

export default function AdminUsersPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [actioningId, setActioningId] = useState(null); // walletId being actioned

  useEffect(() => {
    const loadUsers = async () => {
      setLoading(true);
      setError("");
      try {
        const data = await getAdminUsers(page, 10);
        setUsers(data.content || []);
        setTotalPages(data.totalPages || 0);
      } catch (err) {
        setError("Failed to load users");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    loadUsers();
  }, [page]);

  const handleToggleWallet = async (walletId, currentStatus) => {
    if (!walletId) {
      alert("No wallet found for this user");
      return;
    }

    const isFreezing = currentStatus !== "FROZEN";
    const action = isFreezing ? "freeze" : "unfreeze";

    if (!window.confirm(`Are you sure you want to ${action} this wallet?`))
      return;

    setActioningId(walletId);
    try {
      if (isFreezing) {
        await freezeWallet(walletId);
      } else {
        await unfreezeWallet(walletId);
      }
      // Update local state — flip walletStatus
      setUsers((prev) =>
        prev.map((u) =>
          u.walletId === walletId
            ? { ...u, walletStatus: isFreezing ? "FROZEN" : "ACTIVE" }
            : u
        )
      );
    } catch (err) {
      alert(err?.response?.data?.message || "Action failed");
    } finally {
      setActioningId(null);
    }
  };

  return (
    <AdminLayout>
      <div className="admin-list">
        <h1>👥 User Management</h1>

        {error && <div className="alert alert-error">{error}</div>}

        {loading ? (
          <div style={{ padding: "2rem" }}>Loading users...</div>
        ) : users.length > 0 ? (
          <>
            <div className="list-card">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>Email</th>
                    <th>Phone</th>
                    <th>Total Balance</th>
                    <th>Available</th>
                    <th>Wallet Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((user) => (
                    <tr key={user.userId}>
                      <td>{user.email}</td>
                      <td>{user.phone}</td>
                      <td>{formatINR(user.totalBalance)}</td>
                      <td>{formatINR(user.availableBalance)}</td>
                      <td>
                        <span
                          className={`status-badge ${user.walletStatus.toLowerCase()}`}
                        >
                          {user.walletStatus}
                        </span>
                      </td>
                      <td>
                        {user.walletId ? (
                          <button
                            className="btn btn-small"
                            onClick={() =>
                              handleToggleWallet(user.walletId, user.walletStatus)
                            }
                            disabled={actioningId === user.walletId}
                            style={{
                              color:
                                user.walletStatus === "FROZEN"
                                  ? "#10b981"
                                  : "#ef4444",
                            }}
                          >
                            {actioningId === user.walletId
                              ? "..."
                              : user.walletStatus === "FROZEN"
                              ? "Unfreeze"
                              : "Freeze"}
                          </button>
                        ) : (
                          <span style={{ color: "#9ca3af", fontSize: "0.875rem" }}>
                            No wallet
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="pagination">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="btn btn-small"
              >
                ← Previous
              </button>
              <span>
                Page {page + 1} of {totalPages || 1}
              </span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= (totalPages || 1) - 1}
                className="btn btn-small"
              >
                Next →
              </button>
            </div>
          </>
        ) : (
          <div style={{ padding: "2rem" }}>No users found</div>
        )}
      </div>
    </AdminLayout>
  );
}
