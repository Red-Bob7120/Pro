import { useState, useRef } from "react";
import Header from "./components/Header.jsx";
import TodoEditor from "./components/TodoEditor.jsx";
import Todolist from "./components/Todolist.jsx";
import "./App.css";

const mockTodo = [
  { id: 0, isDone: false, content: "리액트 공부하기", createdDate: Date.now() },
  { id: 1, isDone: false, content: "빨래 널기", createdDate: Date.now() },
  { id: 2, isDone: false, content: "노래 연습하기", createdDate: Date.now() },
];

function App() {
  const idRef = useRef(3);
  const [todo, setTodo] = useState(mockTodo);

  // 완료 토글
  const onUpdate = (targetId) => {
    setTodo((prev) =>
      prev.map((it) =>
        it.id === targetId ? { ...it, isDone: !it.isDone } : it
      )
    );
  };

  // 항목 추가
  const onCreate = (content) => {
    const newItem = {
      id: idRef.current,
      content,
      isDone: false,
      createdDate: Date.now(),
    };
    setTodo((prev) => [newItem, ...prev]);
    idRef.current += 1;
  };

  // (권장) 항목 삭제
  const onDelete = (targetId) => {
    setTodo((prev) => prev.filter((it) => it.id !== targetId));
  };

  return (
    <div className="App">
      <Header />
      <TodoEditor onCreate={onCreate} />
      <Todolist todo={todo} onUpdate={onUpdate} onDelete={onDelete} />
    </div>
  );
}

export default App;
