import { useState, useEffect } from 'react';
import { BASE_URL } from '@/lib/api';
interface ContentDetail {
    uid: string;
    name: string;
    packages: string[] | null;
    versionId: string;
    description: string;
}

interface Module {
    id: number;
    createdAt: string;
    updatedAt: string;
    contentType: string;
    contentDetail: ContentDetail;
    views: number;
    uid: string;
}

type ModulesResponse = Module[];

const useModulesByVersion = (versionUid: string) => {
    const [modules, setModules] = useState<ModulesResponse | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchModules = async () => {
            try {
                const response = await fetch(`${BASE_URL}/api/javadoc/modules?versionUid=${versionUid}`);
                if (!response.ok) {
                    throw new Error('Failed to fetch modules');
                }
                const data: ModulesResponse = await response.json();
                setModules(data);
            } catch (err) {
                setError(err instanceof Error ? err.message : 'Unknown error');
            } finally {
                setLoading(false);
            }
        };

        fetchModules();
    }, [versionUid]);

    return { modules, loading, error };
};

export default useModulesByVersion;
