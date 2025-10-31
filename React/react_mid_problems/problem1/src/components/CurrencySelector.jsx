import "./CurrencySelector.css";

function CurrencySelector({ rates, onRateChange, onConvert }) {
  return (
    <section className="CurrencySelector">
      <div className="CurrencyOption">
        <input
          type="numbers"
          step="0.0001"
          value={rates.USD}
          onChange={(e) => onRateChange("USD", e.target.value)}
        />
        <button onClick={() => onConvert("USD")}>USD로 환전</button>
      </div>

      <div className="CurrencyOption">
        <input
          type="numbers"
          step="0.0001"
          value={rates.EUR}
          onChange={(e) => onRateChange("EUR", e.target.value)}
        />
        <button onClick={() => onConvert("EUR")}>EUR로 환전</button>
      </div>

      <div className="CurrencyOption">
        <input
          type="numbers"
          step="0.01"
          value={rates.JPY}
          onChange={(e) => onRateChange("JPY", e.target.value)}
        />
        <button onClick={() => onConvert("JPY")}>JPY로 환전</button>
      </div>
    </section>
  );
}

export default CurrencySelector;
