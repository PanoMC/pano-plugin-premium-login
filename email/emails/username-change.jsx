import {
    Body,
    Button,
    Container,
    Head,
    Heading,
    Html,
    Img,
    Preview,
    Section,
    Tailwind,
    Text,
} from '@react-email/components';

const baseUrl = process.env.VERCEL_URL
  ? `https://${process.env.VERCEL_URL}`
  : '';

export const UsernameChangeEmail = ({
  oldUsername,
  newMcUsername,
  websiteName,
  websiteLogo,
  setUsernameLink,
}) => (
  <Html>
    <Head />
    <Preview>{'{{TRANSLATION:preview:Your username has been changed}}'}</Preview>
    <Tailwind>
      <Body className="mx-auto my-auto px-2 font-sans">
        <Container className="mx-auto my-[40px] max-w-[600px] p-[40px]">
          {websiteLogo && (
            <Section className="mb-[32px] text-center">
              <Img
                src={'{{websiteLogo}}'}
                width="80"
                height="80"
                alt={'{{websiteName}}'}
                className="mx-auto my-0 rounded-lg"
              />
            </Section>
          )}
          {websiteName && (
            <Section className="mb-[8px] text-center">
              <Heading className="mx-0 my-0 p-0 text-center text-[28px] font-bold text-gray-900">
                {'{{websiteName}}'}
              </Heading>
            </Section>
          )}
          <Heading className="mx-0 my-[32px] p-0 text-left text-[24px] font-semibold text-gray-800">
            {'{{TRANSLATION:title:Username Change Required}}'}
          </Heading>
          <Text className="text-[16px] leading-[24px] text-gray-700">
            {'{{TRANSLATION:hello:Hello}}'} <strong>{'{{oldUsername}}'}</strong>,
          </Text>
          <Text className="text-[16px] leading-[24px] text-gray-700">
            {'{{TRANSLATION:body:A Minecraft premium account has claimed the username you were using. Your username has been temporarily changed. Please set a new username by clicking the button below.}}'}
          </Text>
          <Text className="text-[16px] leading-[24px] text-gray-700">
            {'{{TRANSLATION:new-mc-username:Minecraft username that claimed your name}}'}: <strong>{'{{newMcUsername}}'}</strong>
          </Text>
          <Section className="my-[32px] text-center">
            <Button
              className="rounded-lg bg-[#55C55A] px-8 py-4 text-center text-[16px] font-semibold text-white no-underline shadow-md"
              href={'{{setUsernameLink}}'}
            >
              {'{{TRANSLATION:button:Set New Username}}'}
            </Button>
          </Section>
          <Section className="my-[24px] rounded-lg bg-yellow-50 p-[20px]">
            <Text className="m-0 mb-[8px] text-[14px] font-semibold text-yellow-900">
              {'{{TRANSLATION:expiration-notice:⏰ Expiration Notice}}'}
            </Text>
            <Text className="m-0 text-[14px] leading-[20px] text-yellow-800">
              {'{{TRANSLATION:expiration-body:This link will expire in 15 minutes. After that, you can log in with your email and password and set a new username.}}'}
            </Text>
          </Section>
          <Text className="mt-[16px] text-[14px] leading-[20px] text-gray-500">
            {"{{TRANSLATION:alternative:If you didn't expect this change, you can log in with your email and password. You will be prompted to set a new username.}}"}
          </Text>
          <Section className="mt-[40px] border-t border-gray-200 pt-[24px]">
            <Text className="text-left text-[12px] leading-[18px] text-gray-500">
              {'{{TRANSLATION:footer:If you have any questions, please contact our support team.}}'}
            </Text>
          </Section>
        </Container>
      </Body>
    </Tailwind>
  </Html>
);

UsernameChangeEmail.PreviewProps = {
  oldUsername: 'Steve',
  newMcUsername: 'PremiumSteve',
  websiteName: 'MyMinecraft Server',
  websiteLogo: `${baseUrl}/static/server-logo.png`,
  setUsernameLink: 'https://example.com/set-username?token=abc123',
};

export default UsernameChangeEmail;
