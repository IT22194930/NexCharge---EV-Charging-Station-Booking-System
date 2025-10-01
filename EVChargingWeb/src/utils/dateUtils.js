// Utility functions for date and time formatting in booking system

/**
 * Format booking time range like mobile app: "Jan 15, 2024 - 14:00 to 15:00"
 * @param {string} dateString - The reservation date
 * @param {number} hour - The reservation hour (0-23)
 * @returns {string} Formatted time range
 */
export const formatBookingTimeRange = (dateString, hour) => {
  const date = new Date(dateString);
  const options = {
    year: "numeric",
    month: "short",
    day: "numeric",
  };
  const formattedDate = date.toLocaleDateString("en-US", options);
  const startHour = hour.toString().padStart(2, "0");
  const endHour = ((hour + 1) % 24).toString().padStart(2, "0");
  return `${formattedDate} - ${startHour}:00 to ${endHour}:00`;
};

/**
 * Format booking time range in compact format: "14:00-15:00"
 * @param {number} hour - The reservation hour (0-23)
 * @returns {string} Compact time range
 */
export const formatBookingTimeRangeCompact = (hour) => {
  const startHour = hour.toString().padStart(2, "0");
  const endHour = ((hour + 1) % 24).toString().padStart(2, "0");
  return `${startHour}:00-${endHour}:00`;
};

/**
 * Extract only date part from datetime string
 * @param {string} dateString - The datetime string
 * @returns {string} Date only string
 */
export const extractDateOnly = (dateString) => {
  const date = new Date(dateString);
  return date.toISOString().split("T")[0];
};

/**
 * Get hour options for dropdown (0-23)
 * @returns {Array} Array of hour objects with label and value
 */
export const getHourOptions = () => {
  const hours = [];
  for (let i = 0; i < 24; i++) {
    const hourStr = i.toString().padStart(2, "0");
    const nextHourStr = ((i + 1) % 24).toString().padStart(2, "0");
    hours.push({
      value: i,
      label: `${hourStr}:00 - ${nextHourStr}:00`,
    });
  }
  return hours;
};

/**
 * Convert datetime string to date and hour
 * @param {string} datetimeString - The datetime-local input value
 * @returns {object} Object with date and hour
 */
export const parseDateTimeToDateAndHour = (datetimeString) => {
  const datetime = new Date(datetimeString);
  return {
    date: datetime.toISOString().split("T")[0],
    hour: datetime.getHours(),
  };
};

/**
 * Combine date and hour to create datetime string for display
 * @param {string} dateString - The date string
 * @param {number} hour - The hour (0-23)
 * @returns {string} Datetime string for datetime-local input
 */
export const combineDateAndHour = (dateString, hour) => {
  const date = new Date(dateString);
  date.setHours(hour, 0, 0, 0);
  return date.toISOString().slice(0, 16); // Format for datetime-local input
};
