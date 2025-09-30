using System.Text.Json;
using System.Text.Json.Serialization;
using System.Globalization;

namespace EVChargingAPI.Utils
{
    public class LocalDateTimeConverter : JsonConverter<DateTime>
    {
        public override DateTime Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            var dateString = reader.GetString();
            if (string.IsNullOrEmpty(dateString))
            {
                return DateTime.MinValue;
            }

            // Handle the common formats from client
            var formats = new[]
            {
                "yyyy-MM-ddTHH:mm",           // 2024-01-15T08:00 (web format)
                "yyyy-MM-ddTHH:mm:ss",        // 2024-01-15T08:00:00 (with seconds)
                "yyyy-MM-dd HH:mm",           // 2024-01-15 08:00 (space format)
                "yyyy-MM-dd HH:mm:ss",        // 2024-01-15 08:00:00 (space with seconds)
            };

            foreach (var format in formats)
            {
                if (DateTime.TryParseExact(dateString, format, CultureInfo.InvariantCulture, DateTimeStyles.AssumeLocal, out DateTime result))
                {
                    // Treat the parsed datetime as local time, then convert to UTC for storage
                    // This prevents the automatic timezone conversion issues
                    return DateTime.SpecifyKind(result, DateTimeKind.Local).ToUniversalTime();
                }
            }

            // Fallback to default parsing
            if (DateTime.TryParse(dateString, out DateTime fallbackResult))
            {
                return DateTime.SpecifyKind(fallbackResult, DateTimeKind.Local).ToUniversalTime();
            }

            throw new JsonException($"Unable to convert \"{dateString}\" to DateTime.");
        }

        public override void Write(Utf8JsonWriter writer, DateTime value, JsonSerializerOptions options)
        {
            // Convert UTC back to local time for response
            var localTime = value.Kind == DateTimeKind.Utc ? value.ToLocalTime() : value;
            writer.WriteStringValue(localTime.ToString("yyyy-MM-ddTHH:mm:ss"));
        }
    }
}
