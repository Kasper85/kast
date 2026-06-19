const WebSocket = require("ws");

const TV_IP = "192.168.1.43"; // cambia esto
const ws = new WebSocket(`wss://${TV_IP}:3001`, {
  rejectUnauthorized: false
});
ws.on("open", () => {
  console.log("WebSocket conectado");

  const register = {
    type: "register",
    payload: {
      "forcePairing": false,
      "pairingType": "PROMPT",
      "manifest": {
        "manifestVersion": 1,
        "appVersion": "1.0",
        "signed": {
          "created": "20260618",
          "appId": "com.kastlg.app",
          "vendorId": "kastlg",
          "localizedAppNames": {
            "": "KastLG"
          },
          "localizedVendorNames": {
            "": "KastLG"
          },
          "permissions": [
            "LAUNCH",
            "LAUNCH_WEBAPP",
            "APP_TO_APP",
            "CONTROL_INPUT_MEDIA_PLAYBACK",
            "CONTROL_POWER",
            "READ_INSTALLED_APPS"
          ],
          "serial": "2f930e2d2cfe083771f68e4fe7bb07"
        },
        "permissions": [
          "LAUNCH",
          "LAUNCH_WEBAPP",
          "APP_TO_APP",
          "CONTROL_INPUT_MEDIA_PLAYBACK",
          "CONTROL_POWER",
          "READ_INSTALLED_APPS"
        ],
        "signatures": [
          {
            "signatureVersion": 1,
            "signature": "fake"
          }
        ]
      }
    }
  };

  console.log("Enviando register...");
  ws.send(JSON.stringify(register));
});

ws.on("message", (data) => {
  console.log("Respuesta TV:");
  console.log(data.toString());
});

ws.on("error", (err) => {
  console.error("Error:", err.message);
});

ws.on("close", () => {
  console.log("Conexión cerrada");
});