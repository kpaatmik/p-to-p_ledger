/**
 * NotificationsPage — paginated notifications list.
 */

import { useEffect, useState } from "react";
import Layout from "../components/Layout.jsx";
import {
  getNotifications,
  markNotificationAsRead,
} from "../api/notificationService.js";
import { formatDateTime } from "../utils/date.js";
import "../styles/list-page.css";

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    const loadNotifications = async () => {
      setLoading(true);
      setError("");
      try {
        const data = await getNotifications(page, 20);
        setNotifications(data.content || []);
        setTotalPages(data.totalPages || 0);
      } catch (err) {
        setError("Failed to load notifications");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    loadNotifications();
  }, [page]);

  const handleMarkAsRead = async (id) => {
    try {
      await markNotificationAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, read: true } : n))
      );
    } catch (err) {
      console.error("Failed to mark notification as read:", err);
    }
  };

  return (
    <Layout>
      <div className="list-page">
        <h1>🔔 Notifications</h1>

        {error && <div className="alert alert-error">{error}</div>}

        {loading ? (
          <div className="loading-state">Loading notifications...</div>
        ) : notifications.length > 0 ? (
          <>
            <div className="list-card">
              {notifications.map((notif) => (
                <div
                  key={notif.id}
                  className={`notification-item ${
                    notif.read ? "read" : "unread"
                  }`}
                >
                  <div className="notification-content">
                    <p className="message">{notif.message}</p>
                    <small className="time">
                      {formatDateTime(notif.createdAt)}
                    </small>
                  </div>
                  {!notif.read && (
                    <button
                      onClick={() => handleMarkAsRead(notif.id)}
                      className="btn btn-small"
                    >
                      Mark as read
                    </button>
                  )}
                </div>
              ))}
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
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= totalPages - 1}
                className="btn btn-small"
              >
                Next →
              </button>
            </div>
          </>
        ) : (
          <div className="empty-state">No notifications</div>
        )}
      </div>
    </Layout>
  );
}
