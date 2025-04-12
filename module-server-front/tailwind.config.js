// tailwind.config.js
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      maxWidth: {
        "8xl": "90rem",
      },
      colors: {
        navy: {
          50: "#f0f4fa",
          100: "#dde5f2",
          200: "#c3d0e7",
          300: "#9db2d7",
          400: "#738dc2",
          500: "#5671ad",
          600: "#445891",
          700: "#394776",
          800: "#323c62",
          900: "#2c3553",
          950: "#1d2236",
        },
      },
    },
  },
  plugins: [],
  // CDN에서 추가되는 커스텀 설정과 충돌하지 않도록 중요 설정 유지
  important: true,
};
