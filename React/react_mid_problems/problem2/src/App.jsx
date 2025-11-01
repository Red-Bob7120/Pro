import { useState } from "react";
import "./App.css";
import Header from "./components/Header.jsx";
import ExpenseEditor from "./components/ExpenseEditor.jsx";
import ExpenseSummary from "./components/ExpenseSummary.jsx";

export default function App() {
  return (
  <div className="App">
    <section>
      <Header /> 
    </section>
    <section>
      <ExpenseEditor />
    </section>
    <section>
      <ExpenseSummary/>
    </section>
  </div>
  )

}

