import "./globals.css";
import { Inter } from "next/font/google";
import RootLayout from "@/components/RootLayout";

const inter = Inter({ subsets: ["latin"] });

export const metadata = {
  title: "Docsker - 다양하고 넓은 문서 제공 플랫폼",
  description: "다양하고 쉽게 docs를 제공하는 플랫폼",
};

export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body className={inter.className}>
        <RootLayout>{children}</RootLayout>
      </body>
    </html>
  );
}
