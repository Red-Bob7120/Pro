import { useState } from "react";
import ExpenseItem from "./ExpenseItem";
import "./ExpenseList.css";

const ExpenseList = ({ expenses, onDelete }) => {
  const [filter, setFilter] = useState("all");
  const [search, setSearch] = useState("");

  const filtered = expenses.filter((item) => {
    const matchType =
      filter === "all" ? true : item.type === filter;
    const matchSearch = item.description
      .toLowerCase()
      .includes(search.toLowerCase());
    return matchType && matchSearch;
  });

  return (
    <div className="ExpenseList">
      <h4>수입/지출 내역 📜</h4>
      <div className="filter_search_wrapper">
        <select
          className="filter"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        >
          <option value="all">전체</option>
          <option value="income">수입만</option>
          <option value="expense">지출만</option>
        </select>
        <input
          className="searchbar"
          placeholder="검색어를 입력하세요"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>
      <div className="list_wrapper">
        {filtered.map((item) => (
          <ExpenseItem key={item.id} {...item} onDelete={onDelete} />
        ))}
      </div>
    </div>
  );
};

export default ExpenseList;
