import "./ConversionResult.css";

export default function ConversionResult({result}){

    return(
        <section className="App">
            <h2>
                환전된 금액:
            </h2>
            <h1>
                {result}
            </h1>
        </section>
    )
}