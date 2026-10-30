/**
 * Form validation utilities.
 */

export function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

export function isValidPhone(phone) {
  return /^[0-9]{7,20}$/.test(phone.replace(/\s/g, ""));
}

export function isValidPin(pin) {
  return /^\d{4}$/.test(pin);
}

export function isValidPassword(password) {
  return password && password.length >= 8;
}

export function isValidAmount(amount) {
  const num = parseFloat(amount);
  return !isNaN(num) && num > 0;
}

export function isPositiveDecimal(value) {
  const num = parseFloat(value);
  return !isNaN(num) && num > 0;
}
