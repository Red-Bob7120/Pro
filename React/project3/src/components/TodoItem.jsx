import "./TodoItem.css";
import { useContext } from "react";
import { TodoContext } from "../App";

function TodoItem({ id, content, isDone, createdDate }) {
  const { onUpdate, onDelete } = useContext(TodoContext);

  return (
    <div className="TodoItem">

      <div className="checkbox_col">
        <input
          type="checkbox"
          checked={isDone}
          onChange={() => onUpdate(id)}
        />
      </div>

      <div className="title_col">
        {content}
      </div>

      <div className="date_col">
        {new Date(createdDate).toLocaleDateString()}
      </div>

      <button
        className="btn_col"
        onClick={() => onDelete(id)}
      >
        삭제
      </button>

    </div>
  );
}

export default TodoItem;
