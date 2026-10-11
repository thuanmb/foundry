import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react-native';
import AccountMenuItem from '../AccountMenuItem';

jest.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
    }),
}));

describe('AccountMenuItem', () => {
    it('renders icon and label', async () => {
        await render(<AccountMenuItem icon="🚪" label="Logout" />);
        expect(screen.getByText('🚪')).toBeTruthy();
        expect(screen.getByText('Logout')).toBeTruthy();
    });

    it('calls onPress when pressed', async () => {
        const onPress = jest.fn();
        await render(
            <AccountMenuItem icon="📜" label="Privacy" onPress={onPress} />,
        );
        fireEvent.press(screen.getByText('Privacy'));
        expect(onPress).toHaveBeenCalledTimes(1);
    });

    it('renders chevron when onPress is provided', async () => {
        await render(
            <AccountMenuItem icon="📜" label="Privacy" onPress={jest.fn()} />,
        );
        expect(screen.getByText('›')).toBeTruthy();
    });

    it('does not render chevron when no onPress', async () => {
        await render(<AccountMenuItem icon="ℹ️" label="Version" />);
        expect(screen.queryByText('›')).toBeNull();
    });

    it('has correct accessibility role', async () => {
        await render(
            <AccountMenuItem
                icon="📜"
                label="Privacy"
                onPress={jest.fn()}
            />,
        );
        expect(screen.getByRole('button', { name: 'Privacy' })).toBeTruthy();
    });

    it('is disabled when no onPress', async () => {
        await render(<AccountMenuItem icon="ℹ️" label="Version" />);
        expect(
            screen.getByRole('button', { name: 'Version' }),
        ).toBeDisabled();
    });
});
