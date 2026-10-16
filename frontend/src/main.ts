import { bootstrapApplication } from "@angular/platform-browser";
import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { provideRouter } from "@angular/router";
import { App } from "./app/app";
import { tokenInterceptor } from "./app/core/api";
import { routes } from "./app/routes";
bootstrapApplication(App, {
  providers: [
    provideHttpClient(withInterceptors([tokenInterceptor])),
    provideRouter(routes),
  ],
}).catch(console.error);
