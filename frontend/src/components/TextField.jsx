// A labelled input or textarea that shows the server's validation message under it.
export default function TextField({ id, label, required, optional, error, multiline, ...props }) {
    const Tag = multiline ? "textarea" : "input";
    return (
        <div className="mb-3">
            <label htmlFor={id} className="form-label">
                {label}
                {required && <span className="text-danger"> *</span>}
                {optional && <span className="text-muted small"> (optional)</span>}
            </label>
            <Tag id={id} className={`form-control ${error ? "is-invalid" : ""}`} {...props} />
            {error && <div className="invalid-feedback">{error}</div>}
        </div>
    );
}
