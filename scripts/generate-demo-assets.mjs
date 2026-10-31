// 生成模板示例封面和模拟结果图（SVG）。全部由本项目自行生成，不使用第三方图片，无版权问题。
// 用法：node scripts/generate-demo-assets.mjs
// 输出：
//   backend/src/main/resources/demo/covers/*.svg   封面，由 /api/files/demo/** 公开访问
//   backend/src/main/resources/demo/results/*.svg  结果图模板，结果整理步骤复制到存储目录

import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const OUT_DIR = join(
  dirname(fileURLToPath(import.meta.url)),
  "../backend/src/main/resources/demo/covers",
);

// 每个封面：渐变色 + 一组简单图形。标题由前端叠加显示，图片里不写文字。
const COVERS = [
  { name: "postcard", from: "#ff9a62", to: "#7a3fd1", motif: "sun" },
  { name: "polaroid", from: "#4fd1c5", to: "#2b4c9a", motif: "frames" },
  { name: "retro-poster", from: "#f6d365", to: "#d9480f", motif: "stripes" },
  { name: "spring-festival", from: "#e03131", to: "#7d0a0a", motif: "lantern" },
  { name: "birthday", from: "#f783ac", to: "#7048e8", motif: "confetti" },
  { name: "christmas", from: "#2f9e44", to: "#0b3d1f", motif: "tree" },
  { name: "id-photo", from: "#74c0fc", to: "#1c3d6e", motif: "portrait" },
  { name: "couple", from: "#ffa8a8", to: "#862e9c", motif: "hearts" },
];

const MOTIFS = {
  sun: `
    <circle cx="300" cy="300" r="120" fill="#fff" opacity="0.85"/>
    <path d="M0 560 Q150 470 300 540 T600 520 V800 H0Z" fill="#000" opacity="0.25"/>
    <path d="M0 620 Q200 560 380 620 T600 600 V800 H0Z" fill="#000" opacity="0.35"/>`,
  frames: `
    <g transform="rotate(-8 300 400)"><rect x="150" y="190" width="300" height="360" rx="8" fill="#fff" opacity="0.9"/><rect x="175" y="215" width="250" height="250" fill="#000" opacity="0.2"/></g>
    <g transform="rotate(10 300 400)"><rect x="170" y="250" width="300" height="360" rx="8" fill="#fff" opacity="0.75"/><rect x="195" y="275" width="250" height="250" fill="#000" opacity="0.15"/></g>`,
  stripes: `
    <circle cx="300" cy="420" r="210" fill="#fff" opacity="0.18"/>
    <rect x="0" y="360" width="600" height="28" fill="#fff" opacity="0.5"/>
    <rect x="0" y="420" width="600" height="28" fill="#fff" opacity="0.38"/>
    <rect x="0" y="480" width="600" height="28" fill="#fff" opacity="0.26"/>`,
  lantern: `
    <line x1="300" y1="0" x2="300" y2="230" stroke="#ffd43b" stroke-width="6"/>
    <ellipse cx="300" cy="380" rx="150" ry="170" fill="#ffd43b" opacity="0.9"/>
    <rect x="250" y="200" width="100" height="36" rx="6" fill="#ffd43b"/>
    <rect x="250" y="530" width="100" height="36" rx="6" fill="#ffd43b"/>
    <line x1="300" y1="566" x2="300" y2="680" stroke="#ffd43b" stroke-width="6"/>`,
  confetti: `
    <rect x="210" y="380" width="180" height="200" rx="16" fill="#fff" opacity="0.9"/>
    <rect x="285" y="300" width="30" height="80" rx="8" fill="#ffe066"/>
    <circle cx="120" cy="160" r="14" fill="#fff" opacity="0.7"/><circle cx="470" cy="210" r="18" fill="#ffe066" opacity="0.8"/>
    <circle cx="90" cy="520" r="12" fill="#ffe066" opacity="0.8"/><circle cx="510" cy="560" r="14" fill="#fff" opacity="0.7"/>
    <rect x="420" y="380" width="24" height="10" fill="#fff" opacity="0.7" transform="rotate(30 432 385)"/>`,
  tree: `
    <polygon points="300,150 470,420 130,420" fill="#fff" opacity="0.85"/>
    <polygon points="300,280 500,580 100,580" fill="#fff" opacity="0.7"/>
    <rect x="270" y="580" width="60" height="80" fill="#fff" opacity="0.6"/>
    <circle cx="300" cy="140" r="20" fill="#ffd43b"/>`,
  portrait: `
    <rect x="130" y="160" width="340" height="460" rx="20" fill="#fff" opacity="0.9"/>
    <circle cx="300" cy="330" r="80" fill="#1c3d6e" opacity="0.55"/>
    <path d="M170 600 Q300 430 430 600Z" fill="#1c3d6e" opacity="0.55"/>`,
  hearts: `
    <path d="M230 300 C230 240 140 240 140 310 C140 380 230 430 230 460 C230 430 320 380 320 310 C320 240 230 240 230 300Z" fill="#fff" opacity="0.85"/>
    <path d="M380 380 C380 320 290 320 290 390 C290 460 380 510 380 540 C380 510 470 460 470 390 C470 320 380 320 380 380Z" fill="#fff" opacity="0.6"/>`,
};

function svg({ name, from, to, motif }) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 800" width="600" height="800">
  <defs>
    <linearGradient id="bg-${name}" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="${from}"/>
      <stop offset="1" stop-color="${to}"/>
    </linearGradient>
  </defs>
  <rect width="600" height="800" fill="url(#bg-${name})"/>${MOTIFS[motif]}
</svg>
`;
}

// 模拟生成的结果图：同一套配色和图形，加上"示例成片"的边框和角标，与封面区分。
// 文件名与模板 id 去掉 demo- 前缀后一致，结果整理步骤按模板选取。
function resultSvg({ name, from, to, motif }) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 800" width="600" height="800">
  <defs>
    <linearGradient id="res-${name}" x1="1" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="${from}"/>
      <stop offset="1" stop-color="${to}"/>
    </linearGradient>
  </defs>
  <rect width="600" height="800" fill="#f8f5ef"/>
  <rect x="36" y="36" width="528" height="640" rx="12" fill="url(#res-${name})"/>
  <g transform="translate(36 36) scale(0.88)">${MOTIFS[motif]}
  </g>
  <rect x="36" y="700" width="200" height="14" rx="7" fill="#c9c2b6"/>
  <rect x="36" y="728" width="130" height="10" rx="5" fill="#ddd7cc"/>
  <circle cx="530" cy="730" r="34" fill="none" stroke="${to}" stroke-width="5"/>
  <path d="M514 731 L526 743 L548 717" fill="none" stroke="${to}" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
`;
}

const RESULT_DIR = join(OUT_DIR, "../results");
mkdirSync(OUT_DIR, { recursive: true });
mkdirSync(RESULT_DIR, { recursive: true });
for (const cover of COVERS) {
  writeFileSync(join(OUT_DIR, `${cover.name}.svg`), svg(cover));
  writeFileSync(join(RESULT_DIR, `${cover.name}.svg`), resultSvg(cover));
}
console.log(`生成 ${COVERS.length} 个封面和结果图到 ${dirname(OUT_DIR)}`);
