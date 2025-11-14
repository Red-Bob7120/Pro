import "./TodoList.css";
import { useContext, useMemo, useState } from "react";
import {  TodoStateContext } from "../App";
import TodoItem from "./TodoItem";

function TodoList() {
  const { todo } = useContext(TodoStateContext);

  const [search, setSearch] = useState("");

  const filteredTodo = todo.filter((it) =>
    it.content.toLowerCase().includes(search.toLowerCase())
  );

  const analysis = useMemo(() => {
    const total = todo.length;
    const done = todo.filter((it) => it.isDone).length;
    const notDone = total - done;

    return { total, done, notDone };
  }, [todo]);

  return (
    <div className="Todolist">


      <input
        className="serchbar"
        placeholder="검색어를 입력하세요"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />

      <div className="analysis">
        전체 {analysis.total}개 / 완료 {analysis.done}개 / 미완료 {analysis.notDone}개
      </div>

      <div className="list_wrapper">
        {filteredTodo.map((it) => (
          <TodoItem key={it.id} {...it} />
        ))}
      </div>

    </div>
  );
}

export default TodoList;
