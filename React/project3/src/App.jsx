import {  useCallback, useReducer, useRef } from "react";
import Header from "./components/Header.jsx";
import TodoEditor from "./components/TodoEditor.jsx";
import Todolist from "./components/Todolist.jsx";
import "./App.css";

const mockTodo = [
  { id: 0, isDone: false, content: "리액트 공부하기", createdDate: Date.now() },
  { id: 1, isDone: false, content: "빨래 널기", createdDate: Date.now() },
  { id: 2, isDone: false, content: "노래 연습하기", createdDate: Date.now() },
];

function reducer(state, action) {
  switch (action.type) {
    case "CREATE":
      return [action.newItem, ...state];
    case "UPDATE":
      return state.map((it) =>
        it.id === action.targetId ? { ...it, isDone: !it.isDone } : it
      );
    case "DELETE":
      return state.filter((it) => it.id !== action.targetId);
    default:
      return state;
  }
}

function App() {
  const idRef = useRef(3);
  const [todo, dispatch] = useReducer(reducer, mockTodo);

  const onCreate = (content) => {
    dispatch({
      type: "CREATE",
      newItem: {
        id: idRef.current,
        content,
        isDone: false,
        createdDate: new Date().getTime()
      },
    });
    idRef.current += 1;
  };

  const onUpdate = useCallback((targetId) => {
    dispatch({
      type: "UPDATE",
      targetId,
    });
  },[]);

  const onDelete = useCallback((targetId) => {
    dispatch({
      type: "DELETE",
      targetId,
    });
  },[]);

  return (
    <div className="App">
      <Header />
      <TodoEditor onCreate={onCreate} />
      <Todolist todo={todo} onUpdate={onUpdate} onDelete={onDelete} />
    </div>
  );
}

export default App;
