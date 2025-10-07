import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  publicDir: 'public', // Ensure public folder contents are copied to dist
  build: {
    rollupOptions: {
      external: [],
    },
  },
});
