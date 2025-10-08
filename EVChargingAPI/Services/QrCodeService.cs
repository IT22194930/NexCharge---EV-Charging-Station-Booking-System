/*
 * File: QrCodeService.cs
 * Author: Welikanna S. T. (IT22196910)
 * Description: Service for generating QR code assets for bookings.
 *              Uses QRCoder to produce consistent, scannable QR images.
 * 
 * Features:
 * - Fixed error correction (ECC), quiet zone (margin), and size for reliability
 * - Returns image data (e.g., PNG/Base64) from a compact payload
 * - (Optional) Can compose auxiliary text (NIC/session) beneath the QR if configured
 * 
 * Security: Prefer opaque/signed payloads; avoid embedding sensitive PII directly in the QR content.
 */

using QRCoder;

namespace EVChargingAPI.Services
{
    public class QrCodeService
    {
        // Stateless utility method: safe for concurrent calls (no shared mutable state).
        public string GenerateQrBase64(string payload)
        {
            // Generator builds the QR matrix from the input payload.
            using var generator = new QRCodeGenerator();
            // ECCLevel.Q (~25% error recovery) provides a good balance of robustness vs. density.
            using var data = generator.CreateQrCode(payload, QRCodeGenerator.ECCLevel.Q);
            // Renderer: produces PNG bytes; default draws quiet zones (margins) around the code.
            var pngQr = new PngByteQRCode(data);
            // pixelsPerModule=20 yields a large, crisp image suitable for screen and print.
            var bytes = pngQr.GetGraphic(20);
            // Return as Base64 so callers can embed as data URI or store/transmit as text.
            return Convert.ToBase64String(bytes);
        }
    }
}
