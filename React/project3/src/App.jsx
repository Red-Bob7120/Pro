import { useState, useRef } from "react";
import Header from "./components/Header.jsx";
import TodoEditor from "./components/TodoEditor.jsx";
import Todolist from "./components/Todolist.jsx";
import TestComp from "./components/TestComp.jsx"; 


import "./App.css";

const mockTodo = [
  { id: 0, isDone: false, content: "리액트 공부하기", createdDate: Date.now() },
  { id: 1, isDone: false, content: "빨래 널기", createdDate: Date.now() },
  { id: 2, isDone: false, content: "노래 연습하기", createdDate: Date.now() },
];

function App() {
  const idRef = useRef(3);
  const [todo, setTodo] = useState(mockTodo);



  const onUpdate = (targetId) => {
    setTodo((prev) =>
      prev.map((it) =>
        it.id === targetId ? { ...it, isDone: !it.isDone } : it
      )
    );
  };

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

  const onDelete = (targetId) => {
    setTodo((prev) => prev.filter((it) => it.id !== targetId));
  };

  return (
    <div className="App">
      <TestComp/>
      <Header />
      <TodoEditor onCreate={onCreate} />
      <Todolist todo={todo} onUpdate={onUpdate} onDelete={onDelete} />
    </div>
  );
}

export default App;
