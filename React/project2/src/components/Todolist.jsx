import { useState } from "react";
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

  return (
    <div className="Todolist">
      <h4>Todo List</h4>
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
