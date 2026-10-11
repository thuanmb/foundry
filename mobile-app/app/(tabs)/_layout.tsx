import { useTranslation } from 'react-i18next';
import { Tabs } from 'expo-router';
import Ionicons from '@expo/vector-icons/Ionicons';
import { Platform, StyleSheet, type ColorValue } from 'react-native';
import { colors, typography } from '@/theme';
import { useAppSelector } from '@/redux/hooks';
import { selectFeatures } from '@/redux/selectors';

type IconName = React.ComponentProps<typeof Ionicons>['name'];

function TabIcon({ name, color }: { name: IconName; color: ColorValue }) {
    return <Ionicons name={name} color={color} size={24} />;
}

export default function TabLayout() {
    const { t } = useTranslation();
    const features = useAppSelector(selectFeatures);

    return (
        <Tabs
            screenOptions={{
                headerShown: false,
                tabBarActiveTintColor: colors.primary,
                tabBarInactiveTintColor: colors.textMuted,
                tabBarStyle: styles.tabBar,
                tabBarLabelStyle: styles.tabBarLabel,
            }}
        >
            <Tabs.Screen
                name="home"
                options={{
                    tabBarLabel: t('tabs.home'),
                    tabBarIcon: ({ color, focused }) => (
                        <TabIcon name={focused ? 'home' : 'home-outline'} color={color} />
                    ),
                }}
            />
            <Tabs.Screen
                name="notes"
                options={{
                    tabBarLabel: t('tabs.notes'),
                    href: features.notes ? undefined : null,
                    tabBarIcon: ({ color, focused }) => (
                        <TabIcon name={focused ? 'document-text' : 'document-text-outline'} color={color} />
                    ),
                }}
            />
            <Tabs.Screen
                name="account"
                options={{
                    tabBarLabel: t('tabs.account'),
                    tabBarIcon: ({ color, focused }) => (
                        <TabIcon name={focused ? 'person-circle' : 'person-circle-outline'} color={color} />
                    ),
                }}
            />
        </Tabs>
    );
}

const styles = StyleSheet.create({
    tabBar: {
        backgroundColor: colors.tabBarBackground,
        borderTopColor: colors.tabBarBorder,
        borderTopWidth: StyleSheet.hairlineWidth,
        height: Platform.OS === 'ios' ? 88 : 68,
        paddingBottom: Platform.OS === 'ios' ? 28 : 8,
        paddingTop: 8,
        elevation: 0,
    },
    tabBarLabel: {
        ...typography.caption,
    },
});
