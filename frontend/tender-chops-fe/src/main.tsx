import "./index.css";

import ReactDOM from "react-dom/client";
import AppEntryPoint from "./main.app";
import { Suspense } from "react";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <>
    <Suspense>
      <AppEntryPoint />
    </Suspense>
  </>
);
