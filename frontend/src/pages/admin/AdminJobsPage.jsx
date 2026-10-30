/**
 * AdminJobsPage — last 20 scheduled job runs.
 * Endpoint: GET /admin/jobs
 * Response: { "jobs": [ { id, jobName, startedAt, endedAt, status, recordsProcessed, errorDetails }, ... ] }
 */

import { useEffect, useState } from "react";
import AdminLayout from "../../components/AdminLayout.jsx";
import { getJobLogs } from "../../api/adminService.js";
import { formatDateTime } from "../../utils/date.js";
import "../../styles/admin-list.css";

export default function AdminJobsPage() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadJobs = async () => {
      setLoading(true);
      setError("");
      try {
        // Response shape: { jobs: [...] }
        const data = await getJobLogs();
        setJobs(data.jobs || []);
      } catch (err) {
        setError("Failed to load job logs");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    loadJobs();
  }, []);

  const statusClass = (s) => {
    if (!s) return "";
    const lower = s.toLowerCase();
    if (lower === "success") return "success";
    if (lower === "failed" || lower === "failure") return "frozen"; // red
    return "pending";
  };

  return (
    <AdminLayout>
      <div className="admin-list">
        <h1>⚙️ Scheduled Job Monitor</h1>
        <p style={{ color: "#6b7280", marginBottom: "1.5rem" }}>
          Last 20 job executions
        </p>

        {error && <div className="alert alert-error">{error}</div>}

        {loading ? (
          <div style={{ padding: "2rem" }}>Loading job logs...</div>
        ) : jobs.length > 0 ? (
          <div className="list-card">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Job Name</th>
                  <th>Started</th>
                  <th>Ended</th>
                  <th>Status</th>
                  <th>Records</th>
                  <th>Error</th>
                </tr>
              </thead>
              <tbody>
                {jobs.map((job) => (
                  <tr key={job.id}>
                    <td style={{ fontFamily: "monospace", fontSize: "0.875rem" }}>
                      {job.jobName}
                    </td>
                    <td>{formatDateTime(job.startedAt)}</td>
                    <td>{formatDateTime(job.endedAt)}</td>
                    <td>
                      <span className={`status-badge ${statusClass(job.status)}`}>
                        {job.status}
                      </span>
                    </td>
                    <td>{job.recordsProcessed ?? "—"}</td>
                    <td
                      style={{
                        maxWidth: "200px",
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                        fontSize: "0.75rem",
                        color: job.errorDetails ? "#ef4444" : "#9ca3af",
                      }}
                      title={job.errorDetails || ""}
                    >
                      {job.errorDetails || "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div style={{ padding: "2rem" }}>No job logs available</div>
        )}
      </div>
    </AdminLayout>
  );
}
