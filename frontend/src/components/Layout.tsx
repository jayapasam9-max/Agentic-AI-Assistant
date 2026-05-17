import { NavLink, Outlet } from "react-router-dom";
import { Activity, BarChart3, Github, ListChecks } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";

const REPO_URL = "https://github.com/jayapasam9-max/Agentic-AI-Assistant";

const navItems = [
  { to: "/reviews", label: "Reviews", icon: ListChecks },
  { to: "/metrics", label: "Metrics", icon: BarChart3 },
] as const;

export default function Layout() {
  return (
    <div className="min-h-screen bg-background text-foreground">
      <header className="sticky top-0 z-20 border-b border-border bg-background/80 backdrop-blur">
        <div className="container flex h-14 items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <span className="grid h-8 w-8 place-items-center rounded-md bg-primary text-primary-foreground">
              <Activity className="h-4 w-4" />
            </span>
            <div className="leading-tight">
              <div className="text-sm font-semibold">Agentic Code Review</div>
              <div className="text-xs text-muted-foreground">
                Operator dashboard
              </div>
            </div>
          </div>
          <a href={REPO_URL} target="_blank" rel="noreferrer">
            <Button variant="outline" size="sm">
              <Github className="h-4 w-4" />
              <span className="hidden sm:inline">View on GitHub</span>
            </Button>
          </a>
        </div>
      </header>

      <div className="container grid gap-6 py-6 md:grid-cols-[200px_1fr]">
        <aside className="md:sticky md:top-20 md:self-start">
          <nav className="flex gap-2 md:flex-col">
            {navItems.map(({ to, label, icon: Icon }) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) =>
                  cn(
                    "flex flex-1 items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors md:flex-none",
                    isActive
                      ? "bg-accent text-accent-foreground"
                      : "text-muted-foreground hover:bg-accent hover:text-accent-foreground",
                  )
                }
              >
                <Icon className="h-4 w-4" />
                {label}
              </NavLink>
            ))}
          </nav>
        </aside>

        <main className="min-w-0">
          <Outlet />
        </main>
      </div>

      <footer className="border-t border-border py-6">
        <div className="container text-xs text-muted-foreground">
          Built with React, TypeScript, Tailwind, and TanStack Query. Backend:
          Spring Boot + LangChain4j + Claude.
        </div>
      </footer>
    </div>
  );
}
