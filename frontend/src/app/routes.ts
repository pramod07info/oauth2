import { inject } from "@angular/core";
import { CanActivateFn, Router, Routes } from "@angular/router";
import { map } from "rxjs";
import { Auth } from "./core/api";
const signedIn: CanActivateFn = () => {
  const auth = inject(Auth),
    router = inject(Router);
  return auth
    .load()
    .pipe(map((user) => (user ? true : router.createUrlTree(["/sign-in"]))));
};
const admin: CanActivateFn = () => {
  const auth = inject(Auth),
    router = inject(Router);
  return auth
    .load()
    .pipe(
      map((user) =>
        user?.role === "ADMIN"
          ? true
          : router.createUrlTree([user ? "/dashboard" : "/sign-in"]),
      ),
    );
};
export const routes: Routes = [
  {
    path: "sign-in",
    loadComponent: () => import("./pages/login").then((m) => m.Login),
  },
  {
    path: "register",
    loadComponent: () => import("./pages/register").then((m) => m.Register),
  },
  {
    path: "dashboard",
    canActivate: [signedIn],
    loadComponent: () => import("./pages/dashboard").then((m) => m.Dashboard),
  },
  {
    path: "profile",
    canActivate: [signedIn],
    loadComponent: () => import("./pages/profile").then((m) => m.Profile),
  },
  {
    path: "security",
    canActivate: [signedIn],
    loadComponent: () => import("./pages/security").then((m) => m.Security),
  },
  {
    path: "users",
    canActivate: [admin],
    loadComponent: () => import("./pages/users").then((m) => m.Users),
  },
  {
    path: "users/:id",
    canActivate: [admin],
    loadComponent: () =>
      import("./pages/user-detail").then((m) => m.UserDetail),
  },
  {
    path: "system",
    canActivate: [signedIn],
    loadComponent: () => import("./pages/system").then((m) => m.System),
  },
  { path: "", pathMatch: "full", redirectTo: "dashboard" },
  { path: "**", redirectTo: "dashboard" },
];
