import { useMemo,useState } from "react";
import React from "react";
import TodoItem from "./TodoItem";
import "./Todolist.css";

const Todolist = ({ todo,onUpdate,onDelete }) => {
  const [search, setSearch] = useState("");

  const onChangeSearch = (e) => {
    setSearch(e.target.value);
  };

  const getSearchResults = () => {
    return search === ""
      ? todo
      : todo.filter((it) =>
          it.content.toLowerCase().includes(search.toLowerCase())
        );
  };
  const analyzeTodo = useMemo(()=>{
    const totalCount =todo.length;
    const doneCount = todo.filter((it)=>it.isDone).length;
    const notDoneCount = totalCount - doneCount;
    return{
      totalCount,
      doneCount,
      notDoneCount,
    };
  },[todo]);
const { totalCount,doneCount,notDoneCount}=analyzeTodo;
  return (
    <div className="Todolist">
      <h4>Todo List</h4>
      <div>
        <div>총 개수 : {totalCount}</div>
        <div>완료한 할 일 : {doneCount}</div>
        <div>아직 완료하지 못한 할 일 : {notDoneCount}</div>
      </div>
      <input
        value={search}
        onChange={onChangeSearch}
        className="searchbar"
        placeholder="검색어를 입력하세요"
      />
      <div className="list_wrapper">
        {getSearchResults().map((it) => (
          <TodoItem 
          key={it.id} 
          {...it}
           onUpdate={onUpdate}
           onDelete={onDelete}
            />
        ))}
      </div>
    </div>
  );
};

export default Todolist;
