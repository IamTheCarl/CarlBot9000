import carlbot


class UploadAvatar(carlbot.Module):

    @staticmethod
    async def on_connect():
        path = './CarlBot.png'

        with open(path, 'rb') as image:
            carlbot.term_print_event('Uploading \"{}\" as the profile picture.'.format(path))
            await carlbot.client.edit_profile(avatar=image.read())

    def connection_hooks(self):
        return [self.on_connect]


carlbot.add_module("upload_avatar", UploadAvatar())
