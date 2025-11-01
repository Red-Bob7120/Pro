import { useState, useEffect } from "react";
import "./App.css";
import AmountInput from "./components/AmountInput";
import CurrencySelector from "./components/CurrencySelector";
import ConversionResult from "./components/ConversionResult";

function App() {
  const [amount, setAmount] = useState(1000);
  const [rates, setRates] = useState({
    USD: 0.0007,
    EUR: 0.0006,
    JPY: 0.11,
  });
  const [currentCurrency, setCurrentCurrency] = useState("USD");
  const [result, setResult] = useState("");

  const handleRateChange = (ccy, value) => {
    setRates((prev) => ({
      ...prev,
      [ccy]: parseFloat(value || 0),
    }));
  };

  const handleConvert = (ccy) => {
    setCurrentCurrency(ccy);
  };

  useEffect(() => {
    const converted = (amount * (rates[currentCurrency] ?? 0)).toFixed(2);
    setResult(`${converted} ${currentCurrency}`);
  }, [amount, rates, currentCurrency]);

  return (
    <div className="App">
      <AmountInput amount={amount} setAmount={setAmount} />
      <CurrencySelector
        rates={rates}
        onRateChange={handleRateChange}
        onConvert={handleConvert}
      />
      <ConversionResult result={result} />
    </div>
  );
}

export default App;
