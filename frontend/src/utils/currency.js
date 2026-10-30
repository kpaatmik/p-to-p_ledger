/**
 * Currency formatting utilities for INR (₹).
 * All monetary values are displayed with 4 decimal places.
 */

export function formatINR(value) {
  if (value === null || value === undefined || isNaN(value)) {
    return "₹0.0000";
  }
  const num = parseFloat(value);
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: 4,
    maximumFractionDigits: 4,
  }).format(num);
}

export function parseINR(str) {
  if (!str) return 0;
  return parseFloat(str.replace(/[₹,]/g, ""));
}
