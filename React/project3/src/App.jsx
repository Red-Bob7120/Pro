import React, { useMemo,useReducer, useRef, useCallback } from "react";
import "./App.css";
import Header from "./components/Header";
import TodoEditor from "./components/TodoEditor";
import TodoList from "./components/TodoList";

export const TodoContext = React.createContext();


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


export const TodoStateContext = React.createContext();
export const TodoDispatchContext = React.creacteContext();

function App() {
  const idRef = useRef(3);
  const [todo, dispatch] = useReducer(reducer, mockTodo);

  const onCreate = useCallback((content) => {
    dispatch({
      type: "CREATE",
      newItem: {
        id: idRef.current,
        content,
        isDone: false,
        createdDate: Date.now(),
      },
    });
    idRef.current += 1;
  }, []);

  const onUpdate = useCallback((targetId) => {
    dispatch({
      type: "UPDATE",
      targetId,
    });
  }, []);

  const onDelete = useCallback((targetId) => {
    dispatch({
      type: "DELETE",
      targetId,
    });
  }, []);

  const memoizedDispatches = useMemo(()=>{
    return {onCreate,onDelete,onUpdate}
  })
  return (
    <div className="App">

      <Header />
      <TodoContext.Provider value={ todo }>
        <TodoDispatchContext value ={memoizedDispatches}>
          <TodoEditor />
          <TodoList />
        </TodoDispatchContext>

      </TodoContext.Provider>
    </div>
  );
}

export default App;
