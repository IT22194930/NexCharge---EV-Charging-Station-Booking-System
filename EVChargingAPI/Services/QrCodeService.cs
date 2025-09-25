// Author: Peiris M. H. C. (IT22194930)
// Purpose: Generate QR code Base64 string
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
