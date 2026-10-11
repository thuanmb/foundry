import { useState, useCallback } from "react";
import { getErrorMessage } from "../services/api";

const useApi = (apiFunc) => {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const execute = useCallback(
        async (...args) => {
            setLoading(true);
            setError(null);
            try {
                const response = await apiFunc(...args);
                setData(response.data);
                return response.data;
            } catch (err) {
                setError(getErrorMessage(err));
                throw err;
            } finally {
                setLoading(false);
            }
        },
        [apiFunc]
    );

    const reset = useCallback(() => {
        setData(null);
        setError(null);
        setLoading(false);
    }, []);

    return { data, loading, error, execute, reset };
};

export default useApi;
