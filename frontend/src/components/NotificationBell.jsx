/**
 * NotificationBell — displays unread notification count and quick preview.
 * Shows in the navbar on authenticated screens.
 */

import { useEffect, useState, useRef } from "react";
import { Link } from "react-router-dom";
import { getUnreadCount, getNotifications } from "../api/notificationService.js";
import ROUTES from "../constants/routes.js";
import "../styles/notification-bell.css";

export default function NotificationBell() {
  const [unreadCount, setUnreadCount] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const [recentNotifications, setRecentNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const bellRef = useRef(null);

  useEffect(() => {
    const loadUnreadCount = async () => {
      try {
        const data = await getUnreadCount();
        setUnreadCount(data.count);
      } catch (error) {
        console.error("Failed to load unread count:", error);
      }
    };

    // Load immediately and then every 30 seconds
    loadUnreadCount();
    const interval = setInterval(loadUnreadCount, 30000);
    return () => clearInterval(interval);
  }, []);

  const handleBellClick = async () => {
    setIsOpen(!isOpen);
    if (!isOpen && recentNotifications.length === 0) {
      setLoading(true);
      try {
        const data = await getNotifications(0, 5);
        setRecentNotifications(data.content || []);
      } catch (error) {
        console.error("Failed to load notifications:", error);
      } finally {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (bellRef.current && !bellRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isOpen]);

  return (
    <div className="notification-bell-container" ref={bellRef}>
      <button className="bell-button" onClick={handleBellClick}>
        🔔
        {unreadCount > 0 && <span className="badge">{unreadCount}</span>}
      </button>
      {isOpen && (
        <div className="notification-panel">
          <div className="notification-header">
            <h3>Notifications</h3>
            <Link to={ROUTES.NOTIFICATIONS} className="view-all">
              View All
            </Link>
          </div>
          <div className="notification-list">
            {loading ? (
              <p className="loading">Loading...</p>
            ) : recentNotifications.length > 0 ? (
              recentNotifications.map((notif) => (
                <div key={notif.id} className="notification-item">
                  <p>{notif.message}</p>
                  <small>{new Date(notif.createdAt).toLocaleString()}</small>
                </div>
              ))
            ) : (
              <p className="empty">No notifications</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
