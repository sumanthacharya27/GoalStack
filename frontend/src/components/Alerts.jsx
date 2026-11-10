export default function Alerts({ message, error }) {
    return (
        <>
            {message && <div className="alert alert-success" role="alert">{message}</div>}
            {error && <div className="alert alert-danger" role="alert">{error}</div>}
        </>
    );
}
