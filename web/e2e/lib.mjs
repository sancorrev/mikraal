import { createRequire } from "node:module";
import { readFileSync } from "node:fs";

// Playwright and its browsers are pre-installed outside the project (see README).
const require = createRequire(import.meta.url);
export const { chromium } = require("playwright");

export const BASE_URL = process.env.WEB_URL ?? "http://localhost:3000";
export const desktop = { viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 };
export const mobile = { viewport: { width: 390, height: 844 }, deviceScaleFactor: 1, isMobile: true, hasTouch: true };

const axeSource = readFileSync(require.resolve("axe-core/axe.min.js"), "utf8");

/** Runs axe (WCAG 2.2 A/AA) on the page and returns the violations. */
export async function axe(page) {
  await page.evaluate(axeSource);
  return page.evaluate(async () => {
    const result = await window.axe.run(document, {
      runOnly: { type: "tag", values: ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa"] },
    });
    return result.violations.map((v) => ({
      id: v.id,
      impact: v.impact,
      nodes: v.nodes.map((n) => n.target.join(" ")),
    }));
  });
}

/** Collects uncaught errors and console errors; Next dev noise is ignored. */
export function watchErrors(page) {
  const errors = [];
  page.on("pageerror", (error) => errors.push(`pageerror: ${error.message}`));
  page.on("console", (message) => {
    if (message.type() === "error" && !/Failed to load resource|Download the React DevTools/.test(message.text())) {
      errors.push(`console: ${message.text()}`);
    }
  });
  return errors;
}
