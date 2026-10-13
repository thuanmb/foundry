import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react-native';
import PhoneInput from '../PhoneInput';

const mockPush = jest.fn();

jest.mock('expo-router', () => ({
    useRouter: () => ({
        push: mockPush,
        replace: jest.fn(),
        back: jest.fn(),
    }),
}));

jest.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => ({
            'auth.phoneLabel': 'Phone Number',
            'auth.continue': 'Continue',
            'auth.termsNotice': 'By continuing, you agree.',
        }[key] ?? key),
    }),
}));

describe('PhoneInput', () => {
    it('renders phone label and input', async () => {
        await render(<PhoneInput onSubmit={jest.fn()} submitting={false} />);
        expect(screen.getByText('Phone Number')).toBeTruthy();
        expect(screen.getByText('+84')).toBeTruthy();
        expect(screen.getByText('Continue')).toBeTruthy();
    });

    it('disables continue button when input is empty', async () => {
        await render(<PhoneInput onSubmit={jest.fn()} submitting={false} />);
        expect(screen.getByRole('button', { name: 'Continue' })).toBeDisabled();
    });

    it('shows loading state when submitting', async () => {
        await render(<PhoneInput onSubmit={jest.fn()} submitting={true} />);
        expect(screen.getByRole('button', { name: 'Continue' })).toBeDisabled();
    });

    it('shows terms notice', async () => {
        await render(<PhoneInput onSubmit={jest.fn()} submitting={false} />);
        expect(screen.getByText('By continuing, you agree.')).toBeTruthy();
    });

    it('enables button after entering 9 digits', async () => {
        const onSubmit = jest.fn();
        await render(<PhoneInput onSubmit={onSubmit} submitting={false} />);
        const input = screen.getByPlaceholderText('000 000 000');
        await act(async () => {
            fireEvent.changeText(input, '912345678');
        });
        expect(screen.getByRole('button', { name: 'Continue' })).toBeEnabled();
    });

    it('calls onSubmit with +84 prefix when button pressed', async () => {
        const onSubmit = jest.fn();
        await render(<PhoneInput onSubmit={onSubmit} submitting={false} />);
        const input = screen.getByPlaceholderText('000 000 000');
        await act(async () => {
            fireEvent.changeText(input, '912345678');
        });
        await act(async () => {
            fireEvent.press(screen.getByRole('button', { name: 'Continue' }));
        });
        expect(onSubmit).toHaveBeenCalledWith('+84912345678');
    });

    it('navigates to privacy policy when terms notice is tapped', async () => {
        mockPush.mockClear();

        await render(<PhoneInput onSubmit={jest.fn()} submitting={false} />);
        const termsLink = screen.getByText('By continuing, you agree.');
        await act(async () => {
            fireEvent.press(termsLink);
        });
        expect(mockPush).toHaveBeenCalledWith('/(tabs)/account/privacy');
    });
});
