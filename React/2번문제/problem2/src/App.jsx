import { useState } from "react";
import "./App.css";
import Header from "./components/Header";
import ExpenseEditor from "./components/ExpenseEditor";
import ExpenseSummary from "./components/ExpenseSummary";
import ExpenseList from "./components/ExpenseList";

function App() {
  const [expenses, setExpenses] = useState([
    {
      id: 1,
      type: "expense",
      description: "점심 식사",
      amount: 12000,
      date: "2025-10-28",
    },
    {
      id: 2,
      type: "income",
      description: "용돈",
      amount: 50000,
      date: "2025-10-28",
    },
    {
      id: 3,
      type: "expense",
      description: "커피",
      amount: 4500,
      date: "2025-10-28",
    },
  ]);

  // 항목 추가
  const handleAdd = (newItem) => {
    setExpenses([newItem, ...expenses]);
  };

  // 항목 삭제
  const handleDelete = (id) => {
    setExpenses(expenses.filter((item) => item.id !== id));
  };

  return (
    <div className="App">
      <Header />
      <ExpenseEditor onAdd={handleAdd} />
      <ExpenseSummary expenses={expenses} />
      <ExpenseList expenses={expenses} onDelete={handleDelete} />
    </div>
  );
}

export default App;
