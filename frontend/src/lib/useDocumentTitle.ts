import { useEffect } from "react";

const SUFFIX = "Operator dashboard";

/**
 * Set `document.title` to `"<title> — Operator dashboard"` while the
 * component is mounted, then restore the previous title on unmount.
 *
 * <p>Tiny hook (no deps) so browser tabs show meaningful per-route names
 * for users who Cmd-T between tabs.
 */
export function useDocumentTitle(title: string): void {
  useEffect(() => {
    const previous = document.title;
    document.title = title ? `${title} — ${SUFFIX}` : SUFFIX;
    return () => {
      document.title = previous;
    };
  }, [title]);
}
