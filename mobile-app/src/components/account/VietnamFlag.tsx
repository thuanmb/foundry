import { View, StyleSheet } from 'react-native';

/**
 * Simple Vietnam flag rendered with Views (red background, yellow star).
 * No external image asset needed.
 */
export default function VietnamFlag({ size = 24 }: { size?: number }) {
    const flagHeight = Math.round(size * 0.67);

    return (
        <View
            style={[
                styles.container,
                { width: size, height: flagHeight, borderRadius: 2 },
            ]}
        >
            <View style={styles.star}>
                <StarPath size={Math.round(flagHeight * 0.55)} />
            </View>
        </View>
    );
}

function StarPath({ size }: { size: number }) {
    const points = 5;
    const outerRadius = size / 2;
    const innerRadius = outerRadius * 0.38;
    const center = size / 2;

    const starPoints: string[] = [];
    for (let i = 0; i < points * 2; i++) {
        const radius = i % 2 === 0 ? outerRadius : innerRadius;
        const angle = (Math.PI / 2) * -1 + (Math.PI / points) * i;
        const x = center + radius * Math.cos(angle);
        const y = center + radius * Math.sin(angle);
        starPoints.push(`${x},${y}`);
    }

    return (
        <View style={[styles.starSvg, { width: size, height: size }]}>
            {/* Render star using triangle pairs for each point */}
            {Array.from({ length: points }).map((_, i) => {
                const tipAngle = (Math.PI / 2) * -1 + (2 * Math.PI * i) / points;
                const leftAngle = tipAngle - Math.PI / points;
                const rightAngle = tipAngle + Math.PI / points;

                const tipX = center + outerRadius * Math.cos(tipAngle);
                const tipY = center + outerRadius * Math.sin(tipAngle);
                const leftX = center + innerRadius * Math.cos(leftAngle);
                const leftY = center + innerRadius * Math.sin(leftAngle);
                const rightX = center + innerRadius * Math.cos(rightAngle);
                const rightY = center + innerRadius * Math.sin(rightAngle);

                // Normalize to triangle coordinates within the container
                const minX = Math.min(tipX, leftX, rightX);
                const minY = Math.min(tipY, leftY, rightY);
                const maxX = Math.max(tipX, leftX, rightX);
                const maxY = Math.max(tipY, leftY, rightY);

                return (
                    <View
                        key={i}
                        style={{
                            position: 'absolute',
                            left: minX,
                            top: minY,
                            width: maxX - minX,
                            height: maxY - minY,
                            borderLeftWidth: (maxX - minX) / 2,
                            borderRightWidth: (maxX - minX) / 2,
                            borderBottomWidth: maxY - minY,
                            borderLeftColor: 'transparent',
                            borderRightColor: 'transparent',
                            borderBottomColor: '#FFD700',
                            borderStyle: 'solid',
                        }}
                    />
                );
            })}
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        backgroundColor: '#DA251D',
        justifyContent: 'center',
        alignItems: 'center',
        overflow: 'hidden',
    },
    star: {
        justifyContent: 'center',
        alignItems: 'center',
    },
    starSvg: {
        justifyContent: 'center',
        alignItems: 'center',
    },
});
