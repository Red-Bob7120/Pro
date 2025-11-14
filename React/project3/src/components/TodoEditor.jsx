import "./TodoEditor.css";
import { useContext, useRef, useState } from "react";
import { TodoContext } from "../App";

function TodoEditor() {
  const { onCreate } = useContext(TodoContext);
  const [content, setContent] = useState("");
  const inputRef = useRef();

  const onSubmit = () => {
    if (!content) {
      inputRef.current.focus();
      return;
    }
    onCreate(content);
    setContent("");
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter") onSubmit();
  };

  return (
    <div className="TodoEditor">
      <div className="editor_wrapper">
        <input
          ref={inputRef}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="새 할 일을 입력하세요"
        />
        <button onClick={onSubmit}>추가</button>
      </div>
    </div>
  );
}

export default TodoEditor;
