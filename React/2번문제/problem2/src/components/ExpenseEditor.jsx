import { useState } from "react";
import "./ExpenseEditor.css";

const ExpenseEditor = ({ onAdd }) => {
  const [type, setType] = useState("expense");
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [date, setDate] = useState(
    new Date().toISOString().substring(0, 10)
  );

  const handleSubmit = () => {
    if (!description || !amount) {
      alert("내용과 금액을 입력해주세요.");
      return;
    }

    const newItem = {
      id: Date.now(),
      type,
      description,
      amount: Number(amount),
      date,
    };

    onAdd(newItem);
    setDescription("");
    setAmount("");
  };

  return (
    <div className="ExpenseEditor">
      <h4 className="editor_title">새 항목 추가하기 ✏️</h4>
      <div className="editor_wrapper">
        <select value={type} onChange={(e) => setType(e.target.value)}>
          <option value="expense">지출</option>
          <option value="income">수입</option>
        </select>
        <input
          placeholder="내용을 입력하세요"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <input
          type="number"
          placeholder="금액"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
        />
        <input
          type="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
        />
        <button onClick={handleSubmit}>추가</button>
      </div>
    </div>
  );
};

export default ExpenseEditor;
