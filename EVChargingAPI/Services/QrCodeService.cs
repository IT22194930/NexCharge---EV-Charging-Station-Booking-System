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
        public string GenerateQrBase64(string payload)
        {
            using var generator = new QRCodeGenerator();
            using var data = generator.CreateQrCode(payload, QRCodeGenerator.ECCLevel.Q);
            var pngQr = new PngByteQRCode(data);
            var bytes = pngQr.GetGraphic(20);
            return Convert.ToBase64String(bytes);
        }
    }
}
